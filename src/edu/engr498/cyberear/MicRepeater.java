/********************************************************************************
 * MicRepeater.java by Peter Hall for CyberEar
 * 
 * 
 ********************************************************************************/

package edu.engr498.cyberear;

import java.util.Timer;
import java.util.TimerTask;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.media.audiofx.AcousticEchoCanceler;
import android.media.audiofx.AutomaticGainControl;
import android.media.audiofx.NoiseSuppressor;
import android.os.Build;
import android.os.Bundle;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

/*************************************************************************
 * Class: MicRepeater
 * Superclass: Activity
 * 
 * Echos the mic to the headphones.
 *
 *************************************************************************/
public class MicRepeater extends Activity
{
	private AudioTrack audioTrack;
	private AudioRecord audioRecord;
	private AudioManager audioManager;
	private boolean finished = false;			// when done playing, finished is set to stop while-loop.  Only true long enough to stop.
	private boolean playing = false;			// playing audio.  boolean to toggle effect of button
	private int sampleRate = 44100;
	//private int trackLength = 0; //2048;		// default length of sample array to work with: ~0.05s.  Lengthened by minSize if nec'ary.
	private int minSize;						// minimum size of trackLength necessary; trackLength is lengthened to this if nec'ary.
	private int bufferSize = 128; //512;		// size of buffer when reading from AudioRecord and writing to AudioTrack
	private double average = 0;					// RMS average of signal being sent to AudioTrack
	private double dBs = 0;						// decibel value of signal being sent to speakers.
	private int maxVol;
	
	private SeekBar balControl;
	private SeekBar volControl;
	private TextView averageDisp;				// displays decibels now, not RMS average of signal, using double dBs for value.
	private TextView volDisp;					// displays volume setting by volume number (usually 0-15)
	
	private TextView balRatio;
	private float leftVolume;
	private float rightVolume;
	
	private Timer timer;
	private long period = (long) ( 250 );		// how often to update display, ie the volume bar location and numbers: 250ms
	
	private int countdown = 4;					//auto pressing button - empirically found start up procedure to clear buffers and run smoothly
	
	private Equalizer EQL;						//EQ Left
	private Equalizer EQR;						//EQ Right
	private double[] decibels;					//from intent
	private double[] k_values;					//calculated here.

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_mic_repeater);
		
		/*************************************************************************************************************
		 * Set up volume controls.  Give the hardware controls to this activity for controlling media sounds.
		 * Get AudioManager for setting the Android Media volume.
		 *************************************************************************************************************/
		setVolumeControlStream(AudioManager.STREAM_MUSIC);							// set hardware vol controls to media sounds.
		audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		
		/***********************************************************************************************************
		 * Set up SeekBar volControl for setting the Android Media volume.
		 * Also, give it the initial position based on this volume.
		 ***********************************************************************************************************/
		volControl = (SeekBar) findViewById(R.id.seekBar_vol);
		maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
		volControl.setMax(maxVol);
		volControl.setProgress(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC));
		
		volControl.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
			{
				audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, /*volControl.getProgress()*/ progress, 0);
			}
			@Override
			public void onStartTrackingTouch(SeekBar seekBar)
			{
				// do nothing
			}
			@Override
			public void onStopTrackingTouch(SeekBar seekBar)
			{
				// do nothing
			}
		});
		
		/************************************************************************************************************
		 * Set up SeekBar balControl to control balance.
		 ************************************************************************************************************/
		balControl = (SeekBar) findViewById(R.id.seekBar_bal);
		balControl.setMax(1000);
		balControl.setProgress(500);
		
		leftVolume = balControl.getProgress();
		rightVolume = 1000 - leftVolume;

		balRatio = (TextView) findViewById(R.id.textView5);
		balRatio.setText(leftVolume + ":" + rightVolume);
		balControl.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,boolean fromUser)
			{
				// TODO Auto-generated method stub
				leftVolume = progress; //balControl.getProgress();
				rightVolume = 1000 - leftVolume;
				
				balRatio.setText(leftVolume + ":" +rightVolume);
				if(!finished)
				{
					setLeftRightVolume(leftVolume/1000, rightVolume/1000);
				}
			}
			@Override
			public void onStartTrackingTouch(SeekBar seekBar)
			{
				// TODO Auto-generated method stub
			}
			@Override
			public void onStopTrackingTouch(SeekBar seekBar)
			{
				// TODO Auto-generated method stub
				
			}
		});
		
		/*************************************************************************************************************
		 * Set up text field for displaying --RMS average-- decibels of signal before going to AudioTrack
		 ************************************************************************************************************/
		averageDisp = (TextView) findViewById(R.id.textView2);
		//averageDisp.setText(Double.toString(average));
		averageDisp.setText(Double.toString(dBs));
		
		/*************************************************************************************************************
		 * Set up text field for displaying current volume setting (0-15)
		 *************************************************************************************************************/
		volDisp = (TextView) findViewById(R.id.textView4);
		volDisp.setText(Integer.toString(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)));
		
		/********************************************************************************************************************************
		 * Set up Timer with a TimerTask to run on a periodic schedule every period (250ms; adjust with period field above)
		 * Runs some commands on the UI Thread (as they only affect GUI Views) to update the TextView numbers and vol SeekBar position.
		 ********************************************************************************************************************************/
		timer = new Timer();
		timer.schedule(new TimerTask() {
			@Override
			public void run()
			{
				runOnUiThread(new Runnable() {
				    @Override
				    public void run()
				    {
				    	int vol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
				    	volDisp.setText(Integer.toString(vol));
						//averageDisp.setText(Double.toString(average));
				    	averageDisp.setText(Double.toString(dBs));
						volControl.setProgress(vol);
				    }
				});
			}
		}, 0, period);
		
		calculate_k_values();
	}
	
	@Override
	protected void onResume()
	{
		super.onResume();
		
		int recordLength = 0;
		int trackLength = 0;
		
		if((minSize = AudioRecord.getMinBufferSize(sampleRate,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT)) > recordLength)
			recordLength = minSize;
		
		if((minSize = AudioTrack.getMinBufferSize(sampleRate,AudioFormat.CHANNEL_OUT_STEREO,AudioFormat.ENCODING_PCM_16BIT)) > trackLength)
			trackLength = minSize;
		
		if(Build.VERSION.SDK_INT == Build.VERSION_CODES.JELLY_BEAN_MR1)
		{
			audioRecord = new AudioRecord(MediaRecorder.AudioSource.CAMCORDER, sampleRate, AudioFormat.CHANNEL_IN_MONO,
										  AudioFormat.ENCODING_PCM_16BIT, recordLength*2);
		}
		else if(Build.VERSION.SDK_INT >= 7)
		{
			audioRecord = new AudioRecord(MediaRecorder.AudioSource.CAMCORDER, sampleRate, AudioFormat.CHANNEL_IN_MONO,
										  AudioFormat.ENCODING_PCM_16BIT, recordLength);
		}
		else
		{
			audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, AudioFormat.CHANNEL_IN_MONO,
					  AudioFormat.ENCODING_PCM_16BIT, recordLength);
		}
		
		if(Build.VERSION.SDK_INT == Build.VERSION_CODES.JELLY_BEAN_MR1)
			audioTrack = new AudioTrack( AudioManager.STREAM_MUSIC, sampleRate, AudioFormat.CHANNEL_OUT_STEREO,
				 					 AudioFormat.ENCODING_PCM_16BIT, trackLength*2, AudioTrack.MODE_STREAM);
		else
			audioTrack = new AudioTrack( AudioManager.STREAM_MUSIC, sampleRate, AudioFormat.CHANNEL_OUT_STEREO,
					 AudioFormat.ENCODING_PCM_16BIT, trackLength, AudioTrack.MODE_STREAM);
		
		if(Build.VERSION.SDK_INT >= 16)
			addAcousticEchoCanceler();
		
		if(Build.VERSION.SDK_INT >= 16)
			addNoiseSuppressor();
		
		if(Build.VERSION.SDK_INT >= 16)
			addAutoGainControl();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.mic_repeater, menu);
		return true;
	}
	
	@Override
	protected void onPause()
	{
		super.onPause();
		
		if(playing)
		{
			playing = false;
			finished = true;
		}
		countdown = 4;
		
		audioRecord.release();
	}
	
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	public void addNoiseSuppressor()
	{
		if(NoiseSuppressor.isAvailable())
			NoiseSuppressor.create(audioRecord.getAudioSessionId());
	}
	
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	public void addAcousticEchoCanceler()
	{
		if(AcousticEchoCanceler.isAvailable())
			AcousticEchoCanceler.create(audioRecord.getAudioSessionId());
	}
	
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	public void addAutoGainControl()
	{
		if(AutomaticGainControl.isAvailable())
			AutomaticGainControl.create(audioRecord.getAudioSessionId());
	}
	
	public void playTone()
	{
		bufferSize = 128;
		
		//if(Build.VERSION.SDK_INT == Build.VERSION_CODES.JELLY_BEAN_MR1)
		//	bufferSize = this.trackLength;			//with a short[], this adds a LOT of latency, so eventually delete these commented lines!
		
		//EQL = new Equalizer(bufferSize, 2, 4, 2, 2, 2, 0.1, 0.1);
		//EQR = new Equalizer(bufferSize, 2, 4, 2, 2, 2, 0.1, 0.1);
		//EQL = new Equalizer(bufferSize, 1, 1, 1, 1, 1, 2, 2);
		//EQR = new Equalizer(bufferSize, 0.1, 2, 0.1, 0.1, 0.1, 0.1, 0.1);
		EQL = new Equalizer(bufferSize, k_values[0], k_values[1], k_values[2], k_values[ 3], k_values[ 4], k_values[ 5], k_values[ 6]);
		EQR = new Equalizer(bufferSize, k_values[7], k_values[8], k_values[9], k_values[10], k_values[11], k_values[12], k_values[13]);
		//Equalizer safeEarsEQ = new Equalizer(bufferSize, 1, 1, 1, 1, 0.1, 0.01, 0.01);
		
		short[] samples = new short[bufferSize];		//better latency if a byte[], but short[] gives better RMS response
		short[] samplesStereo = new short[2*bufferSize];
		short[] eqSamplesLeft; // = new short[bufferSize];
		short[] eqSamplesRight;
		int samplesRead;
		
		average = 0;
		dBs = 0;
		short sampleShort = 0;
		long avgSum = 0;

		audioRecord.startRecording();
		audioTrack.play();
		
		int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
		int volumeGradient;
		int j;
		int smoothDist;
		int counter = 0;
		boolean ouch = false;
		
		while(!finished)
		{
			  android.os.Process.setThreadPriority(-19);
			  samplesRead = audioRecord.read(samples, 0, bufferSize);
			  
			  eqSamplesLeft = EQL.equalize(samples);
			  eqSamplesRight = EQR.equalize(samples);
			  
			  /*
			  if(counter > 0)
		      {
		    	  eqSamples = safeEarsEQ.equalize(samples);
		    	  counter--;
		      }
			  else
			  {
				  eqSamples = EQ.equalize(samples);
			  }
			  */
			  
			  
			  //currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
			  // Impulse protection
			  // & collect RMS average
		      for( int i = 0; i < samplesRead; i++ )
		      {
		    	  /*
		    	  if(currentVolume >= maxVol-3)
		    	  {
		    		  if(eqSamples[i] > 9000)
			    		  eqSamples[i] = 9000;
			    	  
			    	  if(eqSamples[i] < -9000)
			    		  eqSamples[i] = -9000;
		    	  }
		    	  else if(currentVolume >= maxVol-6)
		    	  {
		    		  if(eqSamples[i] > 16383)
			    		  eqSamples[i] = 16383;
			    	  
			    	  if(eqSamples[i] < -16384)
			    		  eqSamples[i] = -16384;
		    	  }
		    	  */
		    	  
		    	  samplesStereo[2*i] = eqSamplesLeft[i];
		    	  samplesStereo[2*i + 1] = eqSamplesRight[i];
		    	  
		          // sampleShort is only a fraction of maximum short (32767), as scaled by -1 to 1 sine wave * 0 to 1 amplitude
		    	  sampleShort = (short)(((long)eqSamplesLeft[i] + (long)eqSamplesRight[i])/2);
		          avgSum += ((long)sampleShort)*((long)sampleShort);
		      }
		      average = Math.sqrt( ((double)avgSum)/((double)samplesRead) );
		      avgSum = 0;
		      dBs = LookUpTable.getDb(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC), average);
		      dBs = average;
		      // write root-mean-square average to textView
		      
		      //Impulse protection:
		      if(average > 8000 && !ouch)
		      {
		    	  ouch = true;
		    	  currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
		    	  counter = 100;
		    	  int newVolume = currentVolume - 5;
		    	  if(newVolume < 0)
		    		  newVolume = 1;
		    	  audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0);
		    	  //eqSamples = safeEarsEQ.equalize(eqSamples);
		      }
		      if(counter > 0)
		      {
		    	  counter--;
		    	  if(counter == 0)
		    	  {
		    		  audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume, 0);
		    		  ouch = false;
		    	  }
		      }
		      
		      
		      audioTrack.write(samplesStereo, 0, 2*samplesRead);
		}
		audioTrack.pause();
		audioTrack.flush();
		finished = false;
	}
	
	public void playButtonPressed(View view)
	{
		Button b = (Button) findViewById(R.id.button1);
		
		if(!playing)
		{
			b.setText("Stop Playback");
			playing = true;
			Thread audioT = new Thread(new Runnable() {
	            public void run()
	            {
	            	android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO /*-19*/ );	//slower (or not :( )
	                playTone();
	            }
			});
			audioT.setPriority(Thread.MAX_PRIORITY);	//just do both, because empirically, it's working!
			while(audioT.getPriority() != Thread.MAX_PRIORITY)
			{
				audioT.setPriority(Thread.MAX_PRIORITY);
			}
			audioT.start();
			
			if(Build.VERSION.SDK_INT == Build.VERSION_CODES.JELLY_BEAN_MR1)
				if(countdown > 0)
				{
					--countdown;
					try{
					Thread.sleep(750);
					} catch (InterruptedException e) { }
					playButtonPressed((View)findViewById(R.id.mrlayout));
				}
		}
		else
		{
			playing = false;
			finished = true;
			b.setText("Start Playback");
			
			if(Build.VERSION.SDK_INT == Build.VERSION_CODES.JELLY_BEAN_MR1)
				if(countdown > 0)
				{
					--countdown;
					try{
					Thread.sleep(1000);
					} catch (InterruptedException e) { }
					playButtonPressed((View)findViewById(R.id.mrlayout));
					return;														//very important for recursion to work properly!!!
				}
			
			if(Build.VERSION.SDK_INT == Build.VERSION_CODES.JELLY_BEAN_MR1)
				if(countdown == 0)
					countdown = 4;
		}
	}
	
	public void endActivity(View view)
	{
		finish();
	}
	
	
	public void centerAudio(View view)
	{
		balControl.setProgress(500);
		leftVolume = balControl.getProgress();
		rightVolume = 1000 - leftVolume;
		balRatio.setText(leftVolume+":"+rightVolume);
		if(!finished)
		{
			setLeftRightVolume(leftVolume/1000, rightVolume/1000);
		}
	}
	
	
	public void setLeftRightVolume(float leftVolume, float rightVolume)
	{
		float left;
		float right;
		
		if(leftVolume > rightVolume)
		{
			left = 1.0f;
			right = (float) rightVolume / leftVolume;
		}
		else if(leftVolume < rightVolume)
		{
			right = 1.0f;
			left = (float) leftVolume / rightVolume;
		}
		else
		{
			left = 1.0f;
			right = 1.0f;
		}
		
		audioTrack.setStereoVolume(right, left);
	}
	
	public void calculate_k_values()
	{
		Intent intent = getIntent();
		decibels = intent.getDoubleArrayExtra(SelectUserActivity.EXTRA_TITLE);
		k_values = new double[14];
		
		double lowest_dBL = decibels[0];
		double lowest_dBR = decibels[7];
		double highest_dBL = 0;
		double highest_dBR = 0;
		double A;
		
		for(int i = 0; i < 7; i++)
		{
			if(decibels[i] < lowest_dBL)
				lowest_dBL = decibels[i];
			if(decibels[i] > highest_dBL)
				highest_dBL = decibels[i];
		}
		for(int i = 7; i < 14; i++)
		{
			if(decibels[i] < lowest_dBR)
				lowest_dBR = decibels[i];
			if(decibels[i] > highest_dBR)
				highest_dBR = decibels[i];
		}
		for(int i = 0; i < 7; i++)
		{
			if(decibels[i] > decibels[i+7])
				decibels[i] += decibels[i] - decibels[i+7];
			if(decibels[i+7] > decibels[i])
				decibels[i+7] += decibels[i+7] - decibels[i];
		}
		for(int i = 0; i < 7; i++)
		{
			A = decibels[i] - lowest_dBL;
			k_values[i] = Math.pow(10, A/20);
		}
		for(int i = 7; i < 14; i++)
		{
			A = decibels[i] - lowest_dBR;
			k_values[i] = Math.pow(10, A/20);
		}
	}
	
	/////////////////////////////////////////////////////////////////////////////////////////////////
	// old left up right up functions, replaced with centerAudio()
	///////////////////////////////////////////////////////////////////////////////////////////////////
	
	//right volume up

	public void rightUp(View view)
	{
		balControl.setProgress(balControl.getProgress() + 10);
		leftVolume = balControl.getProgress();
		rightVolume = 1000 - leftVolume;
		balRatio.setText(leftVolume+":"+rightVolume);
		if(!finished)
		{
			setLeftRightVolume(leftVolume/1000, rightVolume/1000);
		}
	}

	//left volume up

	public void leftUp(View view)
	{
		balControl.setProgress(balControl.getProgress() - 10);
		leftVolume = balControl.getProgress();
		rightVolume = 1000 - leftVolume;
		balRatio.setText(leftVolume+":"+rightVolume);
		if(!finished)
		{
			setLeftRightVolume(leftVolume/1000, rightVolume/1000);
		}
	}

}
