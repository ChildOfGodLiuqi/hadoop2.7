package com.bodani.mr.temperature.sort;

import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;

import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.util.Progressable;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

public class CreateTemperatureData extends Configured implements Tool{
	private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	@Override
	public int run(String[] args) throws Exception {
		FileSystem fs = FileSystem.get(URI.create("hdfs://mycluster:8020"),getConf());
        OutputStream out = fs.create(new Path(args[1]),new Progressable() {
			@Override
			public void progress() {
					System.out.println(sdf.format(new Date ())+"...");
			}
		});
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(out));
        for(long i = 0;i < Long.parseLong(args[0]);i++){
        	 bw.write(createTemperatureRecorder());
        	 bw.newLine();
        }
        bw.close();
		return 0;
	}
	public static void main(String[] args) throws Exception {
		int exitCode = ToolRunner.run(new CreateTemperatureData(), args);
		System.exit(exitCode);
	}
	public String createTemperatureRecorder(){
		String data = "1950-01-01 11:21:02	32℃";
		try {
		int year =  new Random().nextInt(20);
		Calendar cd = Calendar.getInstance();
				cd.set(Calendar.YEAR, year + 1900);
			data = sdf.format(cd.getTime())+"\t"+new Random().nextInt(40)+"℃";
		} catch (Exception e) {
		}
		return data;
	}
}
