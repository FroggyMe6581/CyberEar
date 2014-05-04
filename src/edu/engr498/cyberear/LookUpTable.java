package edu.engr498.cyberear;

public class LookUpTable {

	//dB1  =  2.8975(Volume) + 32.413
	//dB = dB1 + 0.0016(RMS)-0.4098
	//Use this
	//dB = 2.8975(Volume) + 32.413+0.0016(RMS)-0.4098
	
	public static double getDb(int volume, double rms){
		
		return (2.8975*(volume) + 32.413+0.0016*(rms)-0.4098);
		
	}
	
}
