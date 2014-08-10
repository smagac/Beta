package com.badlogic.gdx.backends.lwjgl;

import java.awt.EventQueue;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextArea;

import com.esotericsoftware.tablelayout.swing.Table;

/**
 * Error reporting form hooked into a LWJGL app, allowing users to submit error logs to me
 * @author nhydock
 *
 */
public class Report extends JFrame
{
	private static final long serialVersionUID = 1L;

	public Report(Throwable e)
	{
		setTitle("SwingTest");
		setSize(640, 480);
		setLocationRelativeTo(null);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		
		Table table = new Table();
		table.pad(8);
		
		String h = "<html><body style='width:480px; font-weight: normal'>";
		String msg = "Looks like things didn't run as smoothly as planned.<br/>I hope you don't mind too much.<br/>" +
					 "I'd love it if you could send the log to me in an email so I can make sure this doesn't happen again";
		JLabel label = new JLabel(h+msg);
		table.addCell(label).colspan(3).padBottom(10);
		table.row();
		
		StringWriter output = new StringWriter();
		e.printStackTrace(new PrintWriter(output));
		
		JTextArea errArea = new JTextArea(output.toString());
		errArea.setLineWrap(true);
		
		table.addCell(errArea).fill().colspan(3).pad(8).height(300f);
		table.row();
		table.addCell(new JButton("Send Email")).colspan(1).right();
		table.addCell(new JButton("Close")).colspan(1).right();
		
		getContentPane().add(table);
	}
	
	public static void hook(LwjglApplication app)
	{
		Thread.UncaughtExceptionHandler t = new Thread.UncaughtExceptionHandler() {
			
			@Override
			public void uncaughtException(Thread t, Throwable e) {
				final Report r = new Report(e);
				EventQueue.invokeLater(new Runnable() {
					@Override
					public void run () {
						r.setVisible(true);
					}
				});
			}
		};
		
		app.mainLoopThread.setUncaughtExceptionHandler(t);
	}
}