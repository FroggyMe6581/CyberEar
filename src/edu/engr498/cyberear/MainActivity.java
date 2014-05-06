/***************************************************************************
 * MainActivity.java by Peter Hall for CyberEar
 * 
 * Call intents from this class.
 ************************************************************************/

package edu.engr498.cyberear;

import android.os.Bundle;
import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.View;

public class MainActivity extends Activity
{
	static final int PICK_CONTACT_REQUEST = 1;  // The request code
	private String user_name = "No user selected.";
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	@Override
	protected void onRestart()
	{
		super.onRestart();
		
		//user_name = getIntent().getStringExtra(SelectUserActivity.EXTRA_TITLE);
		//Intent intent = new Intent(this, SelectUserActivity.class);
		//startActivity(intent);
	}
	
	/*
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
	    // Check which request we're responding to
	    if (requestCode == PICK_CONTACT_REQUEST) {
	        // Make sure the request was successful
	        if (resultCode == RESULT_OK) {
	            // The user picked a contact.
	            // The Intent's data Uri identifies which contact was selected.

	            // Do something with the contact here (bigger example below)
	        }
	    }
	}
	*/
	
	/************************************************************************************************************************
	 * Callback for when "RUN" button is pressed.  Starts Activity specified by MicRepeater class.
	 * 
	 * Protects against no user being selected.
	 ************************************************************************************************************************/
	public void startAmplifier(View view)
	{
		//if user is selected
		//Intent intent = new Intent(this, MicRepeater.class);
		//startActivity(intent);
		//else
		//	toast that a user needs to be selected.
	}
	
	/************************************************************************************************************************
	 * Callback for when "Select User or Config" button is pressed.  Starts Activity specified by SelectUserActivity class.
	 * 
	 ************************************************************************************************************************/
	public void startHearingTestActivity(View view)
	{
		Intent intent = new Intent(this, SelectUserActivity.class);
		startActivity(intent);
	}

}
