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

		con1.setText("\t\tIf you are new user, please enter your name." +

		"Then, you should complete the Hearing Check." +

		"During the Hearing Check, press 'I Can Hear' button only after " +

		"you hear a sound.");



		//set title 2

		title2.setText("2. Start Hearing Aid");

		con2.setText("\t\tWhen you complete the Hearing Check, you can start the hearing aid." +

		"Press 'Start Playback' buton to start." +

		"You can adjust the volume with 'Volume Bar'." +

		"Also, you can adjust left volume and right volume differently by using the" +


		"'Balance Bar'.");






		title3.setText("3. Manage Names");


		con3.setText("\t\tIf you've done the Hearing Check before, your name will be listed on the screen," +

		"so you will not have to get Hearing Check again." +

		"You can simply click your name and press 'Start Button'." +

		"If you want to delete a name from list, press long the name.");

		
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.instruction, menu);
		return true;
	}

}
