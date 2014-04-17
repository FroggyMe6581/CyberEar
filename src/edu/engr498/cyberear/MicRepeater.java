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
import android.media.audiofx.NoiseSuppressor;
import android.os.Build;
import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
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
	private int trackLength = 0; //2048;		// default length of sample array to work with: ~0.05s.  Lengthened by minSize if nec'ary.
	private int minSize;						// minimum size of trackLength necessary; trackLength is lengthened to this if nec'ary.
	private int bufferSize = 512;				// size of buffer when reading from AudioRecord and writing to AudioTrack
	private double average = 0;					// RMS average of signal being sent to AudioTrack
	private int maxVol;
	
	private SeekBar balControl;
	private SeekBar volControl;
	private TextView averageDisp;				// displays RMS average of signal, using double average for value.
	private TextView volDisp;					// displays volume setting by volume number (usually 0-15)
	
	private TextView balRatio;
	private float leftVolume;
	private float rightVolume;
	
	private Timer timer;
	private long period = (long) ( 250 );		// how often to update display, ie the volume bar location and numbers: 250ms
	
	private int countdown = 4;					//auto pressing button - empirically found start up procedure to clear buffers and run smoothly

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
		 * Set up text field for displaying RMS average of signal before going to AudioTrack
		 ************************************************************************************************************/
		averageDisp = (TextView) findViewById(R.id.textView2);
		averageDisp.setText(Double.toString(average));
		
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
						averageDisp.setText(Double.toString(average));
						volControl.setProgress(vol);
				    }
				});
			}
		}, 0, period);
		
		if((minSize = AudioTrack.getMinBufferSize(sampleRate,AudioFormat.CHANNEL_OUT_MONO,AudioFormat.ENCODING_PCM_16BIT)) > trackLength)
			trackLength = minSize;
		
		if((minSize = AudioRecord.getMinBufferSize(sampleRate,AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT)) > trackLength)
			trackLength = minSize;
		
		if(Build.VERSION.SDK_INT >= 7)
		{
			audioRecord = new AudioRecord(MediaRecorder.AudioSource.CAMCORDER, sampleRate, AudioFormat.CHANNEL_IN_MONO,
										  AudioFormat.ENCODING_PCM_16BIT, trackLength*2);
		}
		else
		{
			audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, AudioFormat.CHANNEL_IN_MONO,
										  AudioFormat.ENCODING_PCM_16BIT, trackLength*2);
		}
		
		
		audioTrack = new AudioTrack( AudioManager.STREAM_MUSIC, sampleRate, AudioFormat.CHANNEL_OUT_MONO,
				 					 AudioFormat.ENCODING_PCM_16BIT, trackLength*2, AudioTrack.MODE_STREAM);
		
		//if(AcousticEchoCanceler.isAvailable())
		//	AcousticEchoCanceler.create(audioRecord.getAudioSessionId());
		
		//if(NoiseSuppressor.isAvailable())
		//	NoiseSuppressor.create(audioRecord.getAudioSessionId());
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
		
		audioRecord.release();
	}
	
	public void playTone()
	{
		bufferSize = this.trackLength;			//tried a shorter bufferSize, like 512 and 256, yet making it the same was best.
		byte[] samples = new byte[bufferSize];
		int samplesRead;
		
		average = 0;
		short sampleShort = 0;
		long avgSum = 0;

		audioRecord.startRecording();
		audioTrack.play();
		
		
		while(!finished)
		{
			  android.os.Process.setThreadPriority(-19);
			  samplesRead = audioRecord.read(samples, 0, bufferSize);
		      // fill samples array
		      for( int i = 0; i < samplesRead; i++ )
		      {
		          // sampleShort is only a fraction of maximum short (32767), as scaled by -1 to 1 sine wave * 0 to 1 amplitude
		          sampleShort = samples[i];
		          avgSum += ((long)sampleShort)*((long)sampleShort);
		      }
		      average = Math.sqrt( ((double)avgSum)/((double)samplesRead) );
		      avgSum = 0;
		      // write root-mean-square average to textView
		      
		      
		      
		      // ship our sample off to AudioTrack
		      audioTrack.write(samples, 0, samplesRead);
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
			
			if(countdown > 0)
			{
				--countdown;
				try{
				Thread.sleep(500);
				} catch (InterruptedException e) { }
				playButtonPressed((View)findViewById(R.id.mrlayout));
			}
		}
		else
		{
			playing = false;
			finished = true;
			b.setText("Start Playback");
			
			if(countdown > 0)
			{
				--countdown;
				try{
				Thread.sleep(500);
				} catch (InterruptedException e) { }
				playButtonPressed((View)findViewById(R.id.mrlayout));
				return;														//very important for recursion to work properly!!!
			}
			
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
