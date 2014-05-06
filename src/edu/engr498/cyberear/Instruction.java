/***************************************************************************
 * Instruction.java by Seung Baek for CyberEar
 * 
 ************************************************************************/

package edu.engr498.cyberear;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.widget.TextView;

public class Instruction extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_instruction);
		
		TextView title1 = (TextView) findViewById(R.id.textView2);
		TextView con1 = (TextView) findViewById(R.id.textView3);
		
		TextView title2 = (TextView) findViewById(R.id.textView4);
		TextView con2 = (TextView) findViewById(R.id.textView5);
		
		TextView title3 = (TextView) findViewById(R.id.textView6);
		TextView con3 = (TextView) findViewById(R.id.textView7);
		
		
		//set title 1

		title1.setText("1. New User");

		//set con1

		con1.setText("\t\tIf you are a new user, please enter your name.\t" +

		"Then you should complete the Hearing Check.\n\n" +

		"During the Hearing Check, press the 'I Can Hear' button only after " +

		"you hear a sound.");



		//set title 2

		title2.setText("2. Start Hearing Aid");

		con2.setText("\t\tWhen you complete the Hearing Check, you can start the hearing aid.\n\n" +

		"Press the 'Start Playback' button to start.\n\n" +

		"You can adjust the volume with the 'Volume Bar'.\n\n" +

		"Also, you can adjust left volume and right volume separately by using the " +

		"'Balance Bar'.\n\n" +
		
		"To lower the treble, tap 'Decrease Hiss' until the desired level is met.  " +
		"To increase bass, tap 'Increase Bass' until the desired level is met.  " + 
		"Press 'Revert' to set the sound quality back to the original setting.");






		title3.setText("3. Manage Names");


		con3.setText("\t\tIf you've done the Hearing Check before, your name will be listed on the screen, " +

		"so you will not have to take the Hearing Check again.  " +

		"You can simply click your name and press the second 'Start Button'.\n\n" +

		"If you want to delete a name from the list, hold down on the name to delete.\n\n" +
		
		"If sound quality is distorted, try retaking the hearing test by typing your name again.");

		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.instruction, menu);
		return true;
	}

}
