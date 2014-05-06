/************************************************
 * Equalizer.java by Peter Hall for CyberEar
 ************************************************/

package edu.engr498.cyberear;

public class Equalizer
{
	private Filter f_125, f_250, f_500, f_1000, f_2000, f_4000, f_8000;
	
	private int bufferSize;
	
	public Equalizer(int bufferSize, double k_125, double k_250, double k_500, double k_1000, double k_2000, double k_4000, double k_8000)
	{
		this.bufferSize = bufferSize;
		
		f_125  = new Filter(bufferSize,  125,  k_125, -1.9622, 0.9629, 0.0001751, 0.0003502, 0.0001751);
		f_250  = new Filter(bufferSize,  250,  k_250, -1.9722, 0.9736, 0.0132, 0, -0.0132);
		f_500  = new Filter(bufferSize,  500,  k_500, -1.9424, 0.9479, 0.0260, 0, -0.0260);
		f_1000 = new Filter(bufferSize, 1000, k_1000, -1.8768, 0.8985, 0.0508, 0, -0.0508);
		f_2000 = new Filter(bufferSize, 2000, k_2000, -1.7241, 0.8063, 0.0969, 0, -0.0969);
		f_4000 = new Filter(bufferSize, 4000, k_4000, -1.3477, 0.6433, 0.1783, 0, -0.1783);
		f_8000 = new Filter(bufferSize, 8000, k_8000, -0.8560, 0.3042, 0.5400, -1.0801, 0.5400);
	}
	
	public short[] equalize(short[] x)
	{
		short[] y = new short[x.length];
		
		short[] y_125  = f_125.filter(x);
		short[] y_250  = f_250.filter(x);
		short[] y_500  = f_500.filter(x);
		short[] y_1000 = f_1000.filter(x);
		short[] y_2000 = f_2000.filter(x);
		short[] y_4000 = f_4000.filter(x);
		short[] y_8000 = f_8000.filter(x);
		
		for(int n = 0; n < x.length; n++)
			y[n] = (short) (y_125[n] + y_250[n] + y_500[n] + y_1000[n] + y_2000[n] + y_4000[n] + y_8000[n]);
		
		return y;
	}
	
	public void adjust_EQ(double k_125, double k_250, double k_500, double k_1000, double k_2000, double k_4000, double k_8000)
	{
		f_125.change_k(k_125);
		f_250.change_k(k_250);
		f_500.change_k(k_500);
		f_1000.change_k(k_1000);
		f_2000.change_k(k_2000);
		f_4000.change_k(k_4000);
		f_8000.change_k(k_8000);
	}
}
