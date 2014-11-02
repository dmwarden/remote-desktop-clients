/**
 * Copyright (C) 2013- Iordan Iordanov
 *
 * This is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this software; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307,
 * USA.
 */


package com.undatech.opaque;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class ConnectionSetupActivity extends Activity {
	private static String TAG = "ConnectionSetupActivity";
	
	private EditText hostname = null;
	private EditText vmname = null;
	private EditText user = null;
	private EditText password = null;
	private Button   saveButton = null;
	private Button   advancedSettingsButton = null;
	
	private Context appContext = null;
	private ConnectionSettings currentConnection = null;
	private String currentSelectedConnection = null;
	private String connectionsList = null;
	private String[] connectionsArray = null;
	private boolean newConnection = false;
	private Spinner layoutMapSpinner = null;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		appContext = getApplicationContext();
		setContentView(R.layout.connection_setup_activity);
		
		hostname = (EditText) findViewById(R.id.hostname);
		vmname   = (EditText) findViewById(R.id.vmname);
		user     = (EditText) findViewById(R.id.user);
		password = (EditText) findViewById(R.id.password);
		
		// Define what happens when one taps the Advanced Settings button.
		advancedSettingsButton = (Button) findViewById(R.id.advancedSettingsButton);
		advancedSettingsButton.setOnClickListener(new OnClickListener () {
			@Override
			public void onClick(View arg0) {
				saveSelectedPreferences(false);
				
				Intent intent = new Intent(ConnectionSetupActivity.this, AdvancedSettingsActivity.class);
				intent.putExtra("com.undatech.opaque.ConnectionSettings", currentConnection);
				startActivityForResult(intent, Constants.ADVANCED_SETTINGS);
			}
		});
		
		// Define what happens when one taps the Connect button.
		saveButton = (Button) findViewById(R.id.saveButton);
		saveButton.setOnClickListener(new OnClickListener () {
			@Override
			public void onClick(View arg0) {
				String u = user.getText().toString();
				String h = hostname.getText().toString();
				
				// Only if a username and a hostname were entered, save the connection and try to connect.
				if (!(u.equals("") || h.equals(""))) {
					saveSelectedPreferences(true);
					finish();
					//Intent intent = new Intent(ConnectionSetupActivity.this, RemoteCanvasActivity.class);
					//intent.putExtra("com.undatech.opaque.ConnectionSettings", currentConnection);
					//startActivity(intent);
				// Otherwise, let the user know that at least a user and hostname are required.
				} else {
					Toast toast = Toast.makeText(appContext, R.string.error_no_user_hostname, Toast.LENGTH_LONG);
					toast.show ();
				}
			}
		});
		
		// Load any existing list of connection preferences.
		loadConnections();
		
		Intent i = getIntent();
		currentSelectedConnection = (String)i.getStringExtra("com.undatech.opaque.connectionToEdit");
		android.util.Log.e(TAG, "currentSelectedConnection SET TO: " + currentSelectedConnection);

		// If no currentSelectedConnection was passed in, then generate one.
		if (currentSelectedConnection == null) {
			currentSelectedConnection = nextLargestNumber(connectionsArray);
			newConnection = true;
		}
		
		currentConnection = new ConnectionSettings (currentSelectedConnection);
		if (newConnection) {
			// Save the empty connection preferences to override any values of a previously
			// deleted connection.
			saveSelectedPreferences(false);
		}
		
		// Finally, load the preferences for the currentSelectedConnection.
		loadSelectedPreferences ();
		
	    // Load list of items from asset folder and populate this:
        List<String> spinnerArray = null;
        try {
            spinnerArray = listFiles("layouts");
        } catch (IOException e) {
            e.printStackTrace();
        }
        layoutMapSpinner = (Spinner) findViewById(R.id.layoutMaps);
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<String> adapter =  new ArrayAdapter<String> (this, android.R.layout.simple_spinner_item, spinnerArray);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        layoutMapSpinner.setAdapter(adapter);
        int selection = spinnerArray.indexOf(currentConnection.getLayoutMap());
        if (selection < 0) {
            selection = spinnerArray.indexOf(Constants.DEFAULT_LAYOUT_MAP);
        }
        layoutMapSpinner.setSelection(selection);
	}
	
	/**
	 * Returns the string representation of N+1 where N is the largest value
	 * in the array "numbers" when converted to an integer.
	 * @return
	 */
	private String nextLargestNumber(String[] numbers) {
		int maxValue = 0;
		if (numbers != null) {
			for (int i = 0; i < numbers.length; i++) {
				int currValue = Integer.parseInt(numbers[i]);
				if (currValue >= maxValue) {
					maxValue = currValue + 1;
				}
			}
		}
		android.util.Log.e(TAG, "nextLargestNumber determined: " + maxValue);
		return Integer.toString(maxValue);
	}
	
	/**
	 * Loads the space-separated string representing the saved connections, splits them,
	 * also setting the appropriate member variables.
	 * @return
	 */
	private void loadConnections() {
		SharedPreferences sp = appContext.getSharedPreferences("generalSettings", Context.MODE_PRIVATE);
		connectionsList = sp.getString("connections", null);
		if (connectionsList != null && !connectionsList.equals("")) {
			connectionsArray = connectionsList.split(" ");
		}
	}
	
	/**
	 * Saves the space-separated string representing the saved connections,
	 * and reloads the list to ensure the related member variables are consistent.
	 */
	private void saveConnections() {
		// Only if this is a new connection do we need to add it to the list
		if (newConnection) {
			newConnection = false;
			
			String newListOfConnections = new String(currentSelectedConnection);
			if (connectionsArray != null) {
				for (int i = 0; i < connectionsArray.length; i++) {
					newListOfConnections += " " + connectionsArray[i];
				}
			}
			
			android.util.Log.d(TAG, "Saving list of connections: " + newListOfConnections);
			SharedPreferences sp = appContext.getSharedPreferences("generalSettings", Context.MODE_PRIVATE);
			Editor editor = sp.edit();
			editor.putString("connections", newListOfConnections.trim());
			editor.apply();
			
			// Reload the list of connections from preferences for consistency.
			loadConnections();
		}
	}
	
	/**
	 * Deletes the currentSelectedConnection from the list of connections and saves it.
	 */
	private void deleteConnection() {
		// Only if this is a new connection do we need to add it to the list
		if (!newConnection) {
			
			String newListOfConnections = new String();
			if (connectionsArray != null) {
				for (int i = 0; i < connectionsArray.length; i++) {
					if (!connectionsArray[i].equals(currentSelectedConnection)) {
						newListOfConnections += " " + connectionsArray[i];
					}
				}
				
				android.util.Log.d(TAG, "Deleted connection, current list: " + newListOfConnections);
				SharedPreferences sp = appContext.getSharedPreferences("generalSettings", Context.MODE_PRIVATE);
				Editor editor = sp.edit();
				editor.putString("connections", newListOfConnections.trim());
				editor.apply();
				
				// Delete the screenshot associated with this connection.
				File toDelete = new File (getFilesDir() + "/" + currentSelectedConnection + ".png");
				toDelete.delete();
				
				// Reload the list of connections from preferences for consistency.
				loadConnections();
			}
		}
	}
	
	/**
	 * This function is used to retrieve data returned by activities started with startActivityForResult.
	 */
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		android.util.Log.i(TAG, "onActivityResult");

		super.onActivityResult(requestCode, resultCode, data);
		switch(requestCode) {
		case (Constants.ADVANCED_SETTINGS):
			if (resultCode == Activity.RESULT_OK) {
				Bundle b = data.getExtras();
				currentConnection = (ConnectionSettings)b.get("com.undatech.opaque.ConnectionSettings");
				saveSelectedPreferences(false);
			} else {
				android.util.Log.i (TAG, "Error during AdvancedSettingsActivity.");
			}
			break;
		}
	}
	
	/**
	 * Loads the preferences from shared preferences and populates the on-screen Views.
	 */
	private void loadSelectedPreferences () {
		// We use the index as the file name to which to save the connection.
		android.util.Log.i(TAG, "Loading current settings from file: " + currentSelectedConnection);
		currentConnection.loadFromSharedPreferences(appContext);
	}
	
	private void updateViewsFromPreferences () {
		hostname.setText(currentConnection.getHostname());
		vmname.setText(currentConnection.getVmname());
		user.setText(currentConnection.getUser());
		password.setText(currentConnection.getPassword());
	}
	
	/**
	 * Saves the preferences which are selected on-screen by the user into shared preferences.
	 */
	private void saveSelectedPreferences(boolean saveInList) {
		android.util.Log.i(TAG, "Saving current settings to file: " + currentSelectedConnection);

		String u = user.getText().toString();
		String h = hostname.getText().toString();

		// Only if a username and a hostname were entered, save the connection to list of connections.
		if (saveInList && !(u.equals("") || h.equals(""))) {
			saveConnections();
		}

		// Then, save the connection to a separate SharedPreferences file.
		currentConnection.setUser(u);
		currentConnection.setHostname(h);
		currentConnection.setVmname(vmname.getText().toString());
		currentConnection.setPassword(password.getText().toString());
		TextView selection = null;
		if (layoutMapSpinner != null) {
		    selection = (TextView) layoutMapSpinner.getSelectedView();
		}
		if (selection != null) {
		    currentConnection.setLayoutMap(selection.getText().toString());
		}
		currentConnection.saveToSharedPreferences(appContext);
	}
	
	@Override
	public void onStop() {
		super.onStop();
		android.util.Log.e(TAG, "onStop");
		//saveSelectedPreferences();
	}
	
	@Override
	public void onResume() {
		super.onResume();
		android.util.Log.e(TAG, "onResume");
		loadSelectedPreferences();
		updateViewsFromPreferences ();
	}
	
	/**
	 * Automatically linked with android:onClick to the add new connection action bar item.
	 * @param view
	 */
	public void deleteConnection (MenuItem menuItem) {
		deleteConnection();
		finish();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.connection_setup_activity_actions, menu);
	    return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem menuItem) {
		int itemID = menuItem.getItemId();
		switch (itemID) {
		case R.id.actionDeleteConnection:
			deleteConnection (menuItem);
			break;
		}
		return true;
	}
	
	private List<String> listFiles(String dirFrom) throws IOException {
        Resources res = getResources();
        AssetManager am = res.getAssets();
        String fileList[] = am.list(dirFrom);

            if (fileList != null)
            {   
                for ( int i = 0;i<fileList.length;i++)
                {
                    Log.d("",fileList[i]); 
                }
            }
        return (List<String>)Arrays.asList(fileList);
    }
}
