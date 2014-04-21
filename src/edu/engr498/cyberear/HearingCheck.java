/*******************************************************
 * HearingCheck.java by Seung Baek for CyberEar
 *******************************************************/

package edu.engr498.cyberear;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
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
import android.content.Intent;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

public class HearingCheck extends Activity
{
	private AudioTrack audioTrack;
	private boolean finished = false;
	private boolean playing = false;
	private int sampleRate = 44100;
	private int sampleLength = 4096;
	private int minSize;
	private double average = 0;
	private double volume = 0;
	private int currentFreq = 500;
	private double currentVolume = 0;
	private boolean hear_button_pressed = true;
	private Thread tone;
	private int[] frequency = new int[7];
	private double[] result = new double[7];
	private int freq_index = 0;
	private String user_name;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_hearing_check);
		Intent intent = getIntent();
		user_name = intent.getStringExtra(SelectUserActivity.EXTRA_TITLE);
		
		for(int i = 0; i<frequency.length; i++)
		{
			frequency[i] = 125*(int)Math.pow(2,i);
		}
		
		if ((minSize = AudioTrack.getMinBufferSize(sampleRate,AudioFormat.CHANNEL_OUT_MONO,AudioFormat.ENCODING_PCM_16BIT)) > sampleLength)
			sampleLength = minSize;
		
		audioTrack = new AudioTrack( AudioManager.STREAM_MUSIC, sampleRate, AudioFormat.CHANNEL_OUT_MONO,
				 					 AudioFormat.ENCODING_PCM_16BIT, sampleLength*2, AudioTrack.MODE_STREAM);
	}

	@Override
	
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.hearing_check, menu);
		return true;
	}
	
	public void playTone(double freq)
	{
		average = 0;
		audioTrack.play(); 
		short[] samples = new short[sampleLength];
		
		double angle = 0;
		double sample = 0;
		short sampleShort = 0;
		//int avgDivisor = 0;
		long avgSum = 0;
		
		
	//	EditText freqInput = (EditText) findViewById(R.id.edit_freq);	
	//	double freq = Double.valueOf(freqInput.getText().toString());

		while(!finished)
		{
		//	  volume = ( (double)volumeControl.getProgress() )/1000.0;
		      // fill samples array
			  volume = currentVolume;
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
		    
		}
		audioTrack.pause();
		audioTrack.flush();
		finished = false;
	}
	
	public void playButtonPressed(final View view)
	{
		Button b = (Button) findViewById(R.id.button1);

		if(!playing)
		{
			b.setText("Stop Playback");
			playing = true;
			
			currentFreq = frequency[freq_index];
			currentVolume = 0;
			
			final Thread tone = new Thread(new Runnable() {	
	            public void run()
	            {
	            	playTone(currentFreq);
	            }
			});
			tone.start();
		
			Timer timer = new Timer();
			timer.schedule(new TimerTask() {
				
				@Override
				public void run()
				{
					if(currentVolume < 1.0){
						currentVolume += 0.05;
					}
					else{
						hear_button_pressed = false;
						canHear(view);
						this.cancel();
					}
				}
				
			}, 0, 2000);
			
		}
		else
		{
			playing = false;
			finished = true;
			b.setText("Start Playback");
		}	
	}
	
	public void canHear(View view){
		final RelativeLayout rl = (RelativeLayout) findViewById(R.id.rlHearingCheck);
		final Button a = (Button) findViewById(R.id.button1);
		final Button b = (Button) findViewById(R.id.button2);

		if(playing){
			//tone = null;
			if(hear_button_pressed)
				result[freq_index] = average;
			else
				result[freq_index] = 0.0;
			
			playing = false;
			finished = true;
			b.setText("Wait");
			
			try {
				//Thread.currentThread();
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			freq_index++;
			if(freq_index==frequency.length){
				RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
				params.addRule(RelativeLayout.CENTER_VERTICAL, RelativeLayout.CENTER_HORIZONTAL);
				b.setText("Done!!!");
				b.setLayoutParams(params);
				addResultToText();
				rl.removeView(a);
			}else{
				b.setText("I Can Hear");
				finished = false;
				playButtonPressed(view);
			}
			
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
			for(int i = 0; i< result.length; i++){
				copy_data += result[i] + " ";
			}
			
			fw = new FileWriter(list);
			fw.write(copy_data.trim());
			fw.close();
			
		} 
		catch (IOException e) {	//no file
			// TODO Auto-generated catch block
			
			e.printStackTrace();
		}
		
		
	/*	
		try {
			fw = new FileWriter(list);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		try {
			name_search = new Scanner(list);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		String copy_data = "";
		while(name_search.hasNext()){
			String aLine = name_search.nextLine();
			String[] line_data = aLine.split("\t");
			
			if(!user_name.equals(line_data[0])){
				copy_data += aLine + "\n";
			}
		}
		
		copy_data += user_name + "\t";
		for(int i = 0; i< result.length; i++){
			copy_data += result[i] + " ";
		}
		try {
			fw.write(copy_data);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		*/
	}

}
