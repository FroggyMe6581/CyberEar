/**********************************************************
 * SelectUserActivity.java by Seung Baek for CyberEar
 **********************************************************/

package edu.engr498.cyberear;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

import android.os.Bundle;
import android.os.Environment;
import android.app.Activity;
import android.content.Intent;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class SelectUserActivity extends Activity
{
	public final static String EXTRA_TITLE = "com.example.hat_demo_1.TITLE";
	private Button start;
	private EditText name_input;
	private String user_name;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_select_user);
		
		start = (Button) findViewById(R.id.button1);
		name_input = (EditText) findViewById(R.id.editText1);
		
		start.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				//start.setText(user_name.getText());
				
				user_name = name_input.getText().toString();
				
				if(userNameCheck(user_name))
				{
					//to the mode-selection page
					
				}
				else
				{
					//to the hearing check page
					Intent intent = new Intent(SelectUserActivity.this, HearingCheck.class);
					String title = name_input.getText().toString();
					intent.putExtra(EXTRA_TITLE, title);
					startActivity(intent);
				}
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.select_user, menu);
		return true;
	}
	
	
	public boolean userNameCheck(String name)
	{
			
		File nameList = new File(Environment.getExternalStorageDirectory(), "nameList.txt");
		Scanner name_check=null;
		FileWriter fw = null;
		String copy_data="";
		
		try {	//scanner exist
			name_check = new Scanner(nameList);
				
			while(name_check.hasNext())
			{
					String aLine = name_check.nextLine();
					String[] line_data = aLine.split("\t");
					copy_data += aLine + "\n";
					if(name.equals(line_data[0]))
					{
						name_check.close();
						return true;
					}
			}
			copy_data += name;
			
			name_check.close();
				
			fw = new FileWriter(nameList);
			fw.write(copy_data);
			fw.close();
			
		} 
		catch (IOException e) {	//no file
			try {
				nameList.createNewFile();
//				fw = new FileWriter(nameList);
//				fw.write(name);
//				fw.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			e.printStackTrace();
		}
		return false;	
		
	}

}
