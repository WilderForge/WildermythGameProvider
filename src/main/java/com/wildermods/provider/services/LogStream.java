package com.wildermods.provider.services;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

class LogStream extends PrintStream {
	private static final Object LOCK = new Object();
	private static final DateTimeFormatter logPrefix = DateTimeFormatter.ofPattern("'['HH:mm:ss.SSS']' ");
	private static FileOutputStream file;
	private final PrintStream out;
	boolean printPrefix = true;
	
	public LogStream(PrintStream out) throws FileNotFoundException {
		super(getLogFile());
		this.out = out;
	}

	@Override
	public void close() {
		super.close();
	}

	@Override
	public void flush() {
		synchronized(LOCK) {
			super.flush();
			out.flush();
		}
	}

	@Override
	public void write(byte[] buf, int off, int len) {
		synchronized(LOCK) {
			super.write(buf, off, len);
			out.write(buf, off, len);
		}
	}

	@Override
	public void write(int b) {
		synchronized(LOCK) {
			super.write(b);
			out.write(b);
		}
	}

	@Override
	public void write(byte[] b) throws IOException {
		synchronized(LOCK) {
			super.write(b);
			out.write(b);
		}
	}
	
	private void prefix() {
		synchronized(LOCK) {
			super.print(logPrefix.format(LocalDateTime.now()));
		}
	}
	
	@Override
	public void print(char c) {
		synchronized(LOCK) {
			if(printPrefix) {
				prefix();
				printPrefix = false;
			}
			if(c == '\n') {
				printPrefix = true;
			}
			super.print(c);
		}
	}
	
	@Override
	public void print(String s) {
		synchronized(LOCK) {
			for(char c : s.toCharArray()) {
				print(c);
			}
		}
	}
	
	@Override
	public void println() {
		synchronized(LOCK) {
			prefix();
			super.println();
		}
	}
	
	@Override
	public void println(boolean x) {
		synchronized(LOCK) {
			prefix();
			super.println(x);
		}
	}
	
	@Override
	public void println(char x) {
		synchronized(LOCK) {
			prefix();
			super.println(x);
		}
	}
	
	@Override
	public void println(int x) {
		synchronized(LOCK) {
			prefix();
			super.println(x);
		}
	}
	
	@Override
	public void println(long x) {
		synchronized(LOCK) {
			prefix();
			super.println(x);
		}
	}
	
	@Override
	public void println(float x) {
		synchronized(LOCK) {
			prefix();
			super.println(x);
		}
	}
	
	@Override
	public void println(double x) {
		synchronized(LOCK) {
			prefix();
			super.println(x);
		}
	}
	
	@Override
	public void println(char[] x) {
		synchronized(LOCK) {
			println(new String(x));
		}
	}
	
	@Override
	public void println(String x) {
		synchronized(LOCK) {
			print(x + "\n");
		}
	}
	
	@Override
	public void println(Object x) {
		synchronized(LOCK) {
			println(x.toString());
		}
	}
	
	private static FileOutputStream getLogFile() {
		if(file != null) {
			return file;
		}
		File file = new File("./logs/" + DateTimeFormatter.ISO_DATE.format(LocalDateTime.now()) + " (1).log");
		for(int i = 2; file.exists(); i++) {
			file = new File("./logs/" + DateTimeFormatter.ISO_DATE.format(LocalDateTime.now()) + " (" + i + ").log");
		}
		try {
			file.getParentFile().mkdirs();
			file.createNewFile();
			LogStream.file = new FileOutputStream(file);
		} catch (IOException e) {
			System.err.println(file.getAbsolutePath());
			throw new AssertionError(e);
		}

		return LogStream.file;
	}
	
}
