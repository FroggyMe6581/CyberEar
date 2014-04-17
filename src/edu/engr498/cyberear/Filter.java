/*********************************************
 * Filter.java by Peter Hall for CyberEar
 *********************************************/

package edu.engr498.cyberear;

public class Filter
{
	private int bufferSize;
	
	private int freq;
	private double k;
	
	private double a0, a1, a2;
	private double b0, b1, b2;
	
	private double b0xK, b1xK, b2xK;
	
	private byte prev_ym1 = 0;
	private byte prev_ym2 = 0;
	
	private byte prev_xm1 = 0;
	private byte prev_xm2 = 0;
	
	public Filter(int bufferSize, int freq, double k, double a1, double a2, double b0, double b1, double b2)
	{
		this.bufferSize = bufferSize;
		
		this.freq = freq;
		this.k = k;
		
		this.a0 = 1;
		this.a1 = a1;
		this.a2 = a2;
		
		this.b0 = b0;
		this.b1 = b1;
		this.b2 = b2;
		
		b0xK = b0*k;
		b1xK = b1*k;
		b2xK = b2*k;
	}
	
	public byte[] filter(byte[] x)
	{
		byte[] y = new byte[bufferSize];
		
		//n = 0
		y[0] = (byte)(b0xK*(double)x[0] + b1xK*(double)prev_xm1 + b2xK*(double)prev_xm2 - a1*(double)prev_ym1 - a2*(double)prev_ym2);
		//n = 1
		y[1] = (byte)(b0xK*(double)x[1] + b1xK*(double)x[0] + b2xK*(double)prev_xm1 - a1*(double)y[0] - a2*(double)prev_ym1);
		
		
		for(int n = 2; n < bufferSize; n++)
			y[n] = (byte)(b0xK*(double)x[n] + b1xK*(double)x[n-1] + b2xK*(double)x[n-2] - a1*(double)y[n-1] - a2*(double)y[n-2]);
		
		
		prev_xm1 = x[bufferSize-1];
		prev_xm2 = x[bufferSize-2];
		
		prev_ym1 = y[bufferSize-1];
		prev_ym2 = y[bufferSize-2];
		
		return y;
	}
}
