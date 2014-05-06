/*******************************************************
 * HearingCheck.java by Seung Baek for CyberEar
 *******************************************************/

package edu.engr498.cyberear;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.os.Environment;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;

public class HearingCheck extends Activity
{
	public final static String EXTRA_TITLE = "com.example.hat_demo_1.TITLE";
	private AudioTrack audioTrack;
	private AudioManager audioManager;
	private boolean finished = false;
	private int sampleRate = 44100;
	private int sampleLength = 4096;
	private int minSize;
	private double average = 0;
	private double volume = 0;
	private double currentVolume = 0;
	
	private ImageView iv;
	private Thread tone;
	private int[] frequency = new int[7];
	private double[] result_left = new double[7];
	private double[] result_right = new double[7];
	private double[] result = new double[14];
	
	private int[] volume_left = new int[7];
	private int[] volume_right = new int[7];
	private int[] volume_result = new int[14];
	
	private double[] db_left = new double[7];
	private double[] db_right = new double[7];
	private double[] db_result = new double[14];
	
	private boolean left_done = false;
	
	private int freq_index = 0;
	
	private String user_name;
	
	private ProgressBar pb;
	private final int one_progress = 100/14;
	
	private Button quit_button;
	private boolean can_hear_pressed = false;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_hearing_check);
		
		Intent intent = getIntent();
		user_name = intent.getStringExtra(SelectUserActivity.EXTRA_TITLE);
		
		//initialize progress bar
		pb = (ProgressBar) findViewById(R.id.progressBar1);
		pb.setProgress(0);
		
		for(int i = 0; i<frequency.length; i++)
		{
			frequency[i] = 125*(int)Math.pow(2,i);
		}
		
		setVolumeControlStream(AudioManager.STREAM_MUSIC);
		audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		int init_volume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)-7;

		if(init_volume<=0) 
			init_volume = 1;
		
		audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 
				init_volume, 0);	
		
		if ((minSize = AudioTrack.getMinBufferSize(sampleRate,AudioFormat.CHANNEL_OUT_MONO,AudioFormat.ENCODING_PCM_16BIT)) > sampleLength)
			sampleLength = minSize;
		audioTrack = new AudioTrack( AudioManager.STREAM_MUSIC, sampleRate, AudioFormat.CHANNEL_OUT_MONO,
				 AudioFormat.ENCODING_PCM_16BIT, sampleLength*2, AudioTrack.MODE_STREAM);

		iv = (ImageView) findViewById(R.id.imageView1);
		
		playButtonPressed(this.getCurrentFocus());
		
		quit_button = (Button) findViewById(R.id.button1);
		quit_button.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				finished = true;
				left_done = false;
				freq_index = 0;
				//Intent intent = new Intent(HearingCheck.this, MainActivity.class);
				//startActivity(intent);
				finish();
			}
		});
		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.hearing_check, menu);
		return true;
	}
	
	public void playTone()
	{
		average = 0;

		audioTrack.play(); 
		short[] samples = new short[sampleLength];
		
		double angle = 0;
		double sample = 0;
		short sampleShort = 0;
		
		long avgSum = 0;
		

		while(!finished)
		{
		//	  volume = ( (double)volumeControl.getProgress() )/1000.0;
		      // fill samples array
			
			  volume = currentVolume;
			  double freq = frequency[freq_index];
		      for( int i = 0; i < sampleLength; i++ )
		      {
		    	  //avgDivisor++;
		    	  // volume from 0 to 1, use a fraction of sine wave
		          sample = Math.sin(angle) * volume;
		          // sampleShort is only a fraction of maximum short (32767), as scaled by -1 to 1 sine wave * 0 to 1 volume
		          sampleShort = (short) (sample * Short.MAX_VALUE);
		          avgSum += ((long)sampleShort)*((long)sampleShort);

		          samples[i] = sampleShort;
		          
		          // advance the sine wave based on frequency
		          angle += 2 * Math.PI * freq / sampleRate;  // same as angle = 2pi*f*i/sampleRate

		          // this is to prevent angle from overflowing
		          if (angle > (2 * Math.PI)) angle -= (2 * Math.PI);
		      }
		      average = Math.sqrt( ((double)avgSum)/((double)sampleLength) );
		      avgSum = 0;
		      // write root-mean-square average to textView
		      
		      
		      
		      // ship our sample off to AudioTrack
		      audioTrack.write(samples, 0, sampleLength);
		      
		      while(can_hear_pressed){
		    	  audioTrack.setStereoVolume(0, 0);
		      }
		      	if(!left_done){
		    	
		      		audioTrack.setStereoVolume(1.0f, 0);
		      	}
		      	else{
		    	
		      		audioTrack.setStereoVolume(0, 1.0f);
		      	}
		     
		}
		audioTrack.pause();
		audioTrack.flush();
		finished = false;
	}
	
	public void playButtonPressed(final View view)
	{
			tone = new Thread(new Runnable() {	
	            public void run()
	            {
	            	playTone();
	            	
	            }
			});
			tone.start();
			
			Timer timer = new Timer();
			timer.schedule(new TimerTask() {
				
				@Override
				public void run()
				{			
					audioTrack.flush();
					audioTrack.play();
					
					Timer tt = new Timer();
					tt.schedule(new TimerTask(){

						@Override
						public void run() {
							// TODO Auto-generated method stub
							audioTrack.pause();
							
						}
						
					}, 500);	
					
					
					if(currentVolume < 1.0){
						
						currentVolume += 0.05;
						
					}
					else{
						
						runOnUiThread(new Runnable(){			
							@Override
							public void run() {
								// TODO Auto-generated method stub
								
								canHear(view);
							}
						});
						currentVolume = 0;
					}
					
					if(can_hear_pressed)
						can_hear_pressed = false;
				}	
			}, 0,2000);	//2s for one frequency play sound
		
	}
	
	
	public void canHear(View view){
			can_hear_pressed = true;
			int vol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
			currentVolume = 0;
			
			if(!left_done){
				result_left[freq_index] = average;
				volume_left[freq_index] = vol;
				db_left[freq_index] = LookUpTable.getDb(vol, average);
			}
			else{
				result_right[freq_index] = average;
				volume_right[freq_index] = vol;
				db_right[freq_index] = LookUpTable.getDb(vol, average);
			}
			pb.setProgress(pb.getProgress()+one_progress);
		
			
			if(!left_done){
				
				left_done = true;
				iv.setBackgroundResource(R.drawable.right_sound);
				
			}
			else{
				
				left_done = false;
				iv.setBackgroundResource(R.drawable.left_sound);
				freq_index++;
				
			}
			
			if(freq_index==frequency.length){
				
				pb.setProgress(pb.getProgress()+one_progress);
					freq_index =0;
					addResultToText();
					
					finished = true;
	
					//send result to MicRepeater
					Intent intent = new Intent(HearingCheck.this, MicRepeater.class);
					intent.putExtra(EXTRA_TITLE, db_result);
					startActivity(intent);
					finish();
					
			}	
			
	}
	
	public void addResultToText(){
		
		File list;
		if(Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()))
		{
			list = new File(Environment.getExternalStorageDirectory(), "nameList.txt");
		}
		else
		{
			list = new File(this.getFilesDir(), "nameList.txt");
		}
		
		Scanner name_search = null;
		FileWriter fw = null;
		String copy_data = "";
		
		try {	//scanner exist
			name_search = new Scanner(list);
				
			while(name_search.hasNext()){
					String aLine = name_search.nextLine();
					String[] line_data = aLine.split("\t");
					
					if(!user_name.equals(line_data[0])){
						copy_data += aLine + "\n";
				}
			}
			
			copy_data += user_name + "\t";
			for(int i = 0; i< result_left.length; i++){
				//copy_data += result_left[i] + " ";
				//copy_data += volume_left[i] + " ";
				copy_data += db_left[i] + " ";
				result[i] = result_left[i];				//add to result array
				volume_result[i] = volume_left[i];	//add volume to volume_result
				db_result[i] = db_left[i];
			}
			
			copy_data = copy_data.trim() + "\t";
			for(int i = 0; i< result_left.length; i++){
				//copy_data += result_right[i] + " ";
				//copy_data += volume_right[i] + " ";
				copy_data += db_right[i] + " ";
				result[i+7] = result_right[i];			//add to result array
				volume_result[i+7] = volume_right[i];	//add volume to volume_result
				db_result[i+7] = db_right[i];
			}
			
			fw = new FileWriter(list);
			fw.write(copy_data.trim());
			fw.close();
			
		} 
		catch (IOException e) {	//no file
			// TODO Auto-generated catch block
			
			e.printStackTrace();
		}
		
		
	
	}

}
