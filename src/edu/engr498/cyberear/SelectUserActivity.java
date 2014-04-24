/**********************************************************
 * SelectUserActivity.java by Seung Baek for CyberEar
 **********************************************************/

package edu.engr498.cyberear;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

import android.R.color;
import android.os.Bundle;
import android.os.Environment;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;

public class SelectUserActivity extends Activity
{
	public final static String EXTRA_TITLE = "com.example.hat_demo_1.TITLE";
	private Button start;
	private EditText name_input;
	private String user_name;
	private ArrayList<String[]> data = null;

	
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
					String[] user_data = getUserData(user_name);
					if(user_data!=null){
						double[] result = new double[14];
						String[] left = user_data[1].split(" ");
						String[] right = user_data[2].split(" ");
						for(int i=0; i<7; i++){
							result[i] = Double.parseDouble(left[i].trim());
							result[i+7] = Double.parseDouble(right[i].trim());
						}
						
						//to do - send result to MicRepeater activity
						Intent intent = new Intent(SelectUserActivity.this, MicRepeater.class);
						intent.putExtra(EXTRA_TITLE, result);
						
						
					}
					else{	//name found but no hearing check data
							//send to hearing check page
						Intent intent = new Intent(SelectUserActivity.this, HearingCheck.class);
						String title = name_input.getText().toString();
						intent.putExtra(EXTRA_TITLE, title);
						startActivity(intent);
					}
					
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
		
		data = getNameList();
		final Button start2 = (Button) findViewById(R.id.button2);
		if(data!=null){
			final RadioGroup names = (RadioGroup) findViewById(R.id.radioGroup1);
			RadioButton[] rb = new RadioButton[data.size()];
			for(int i=0; i<rb.length; i++){
				rb[i] = new RadioButton(this);
				names.addView(rb[i]);
				rb[i].setText(data.get(i)[0]);
				rb[i].setTextColor(Color.WHITE);
			}
		
			
			start2.setOnClickListener(new View.OnClickListener() {
			
				@Override
				public void onClick(View v) {
					// TODO Auto-generated method stub
					double[] result = new double[14];
					String[] user_data = data.get(names.getCheckedRadioButtonId());
					String[] left = user_data[1].split(" ");
					String[] right = user_data[2].split(" ");
					for(int i=0; i<7; i++){
						result[i] = Double.parseDouble(left[i].trim());
						result[i+7] = Double.parseDouble(right[i].trim());
					}
					
					//to do - send result to MicRepeater activity
					Intent intent = new Intent(SelectUserActivity.this, MicRepeater.class);
					intent.putExtra(EXTRA_TITLE, result);
					startActivity(intent);
				}
			});
		}
		else{
			findViewById(R.id.textView3).setVisibility(View.GONE);
			start2.setVisibility(View.GONE);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.select_user, menu);
		return true;
	}
	
	@SuppressWarnings("resource")
	public String[] getUserData(String name){
		File nameList = new File(Environment.getExternalStorageDirectory(), "nameList.txt");
		Scanner name_check=null;
		
		try {	//scanner exist
			name_check = new Scanner(nameList);
			
			while(name_check.hasNext())
			{
					String aLine = name_check.nextLine();
					String[] arr = aLine.split("\t");
					if(arr.length==3 && arr[0].equals(name))
						return arr;
			}
			
			name_check.close();
			return null;
			
		} 
		catch (IOException e) {	//no file
			
			e.printStackTrace();
			return null;
		}
	}
	
	public ArrayList<String[]> getNameList(){
		
		File nameList = new File(Environment.getExternalStorageDirectory(), "nameList.txt");
		Scanner name_check=null;
		
		try {	//scanner exist
			name_check = new Scanner(nameList);
			ArrayList<String[]> result = new ArrayList<String[]>();
			while(name_check.hasNext())
			{
					String aLine = name_check.nextLine();
					String[] arr = aLine.split("\t");
					if(arr.length==3)
						result.add(arr);
			}
			
			name_check.close();
			return result;
			
		} 
		catch (IOException e) {	//no file
			
			e.printStackTrace();
			return null;
		}
		
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

			} catch (IOException e1) {
				e1.printStackTrace();
			}
			e.printStackTrace();
		}
		return false;	
		
	}

}
