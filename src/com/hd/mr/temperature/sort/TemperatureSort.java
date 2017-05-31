package com.hd.mr.temperature.sort;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.compress.CompressionCodec;
import org.apache.hadoop.io.compress.GzipCodec;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.SequenceFileOutputFormat;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import util.ConfigurationUtil;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * 统计各年份温度 并 排序
 * hadoop jar temperature-0.0.1.jar com.hadoop.mr.temperature.TemperatureSort /temperature /out/temperatureout/
 */
public class TemperatureSort  {
	/**
	 * 
	 * @param args 0 输入路径，输出路径 ，ReduceTask数量
	 * @throws IOException
	 * @throws ClassNotFoundException
	 * @throws InterruptedException
	 */
    public static void main(String[] args) throws IOException, ClassNotFoundException, InterruptedException {
        Configuration conf = ConfigurationUtil.getConfigurationHA();
        JobConf jconf = new JobConf(conf);
        //对Map输出进行压缩
//        jconf.setBoolean("mapred.compress.map.output", true);  
//        jconf.setClass("mapred.map.output.compression.codec",GzipCodec.class, CompressionCodec.class);
        Job job = Job.getInstance(jconf);

        job.setJarByClass(TemperatureSort.class);

        job.setMapperClass(HotMapper.class);
        //满足  k2,v2 和  k3,v3 类型一一对应时  可以省略
        job.setMapOutputKeyClass(KeyPari.class);
        job.setMapOutputValueClass(Text.class);
        
        FileInputFormat.setInputPaths(job, new Path(args[0]));
        
        job.setCombinerClass(HotReducer.class);
        job.setReducerClass(HotReducer.class);

        job.setOutputKeyClass(KeyPari.class);
        job.setOutputValueClass(Text.class);
        
        
        FileOutputFormat.setOutputPath(job, new Path(args[1]));
        //压缩输出数据
//        FileOutputFormat.setCompressOutput(job, true);
//        FileOutputFormat.setOutputCompressorClass(job, GzipCodec.class);
        //设置分区类
        job.setPartitionerClass(YearPartition.class);
        //设置排序类
        job.setSortComparatorClass(SortHot.class);
        //设置分组类
        job.setGroupingComparatorClass(Group.class);
        //设置分区时  需要多个Reducer 返回多个分区
        //需要设置启动多个Reducer
        //执行后会将结果返回到多个结果文件中
        //如果Reducer数量多于分区数量  则多于 的结果文件为空
        job.setNumReduceTasks(Integer.parseInt(args[2]));

        job.waitForCompletion(true);
    }
    public static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    static class HotReducer extends Reducer<KeyPari,Text,KeyPari,Text>{
        @Override
        protected void reduce(KeyPari key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
            for (Text v:values){
                context.write(key,v);
            }
        }
    }
    static class HotMapper extends Mapper<LongWritable,Text,KeyPari,Text>{
        KeyPari keyPari = new KeyPari();
        @Override
        protected void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
            String line = value.toString();
            String[] ss = line.split("\t");
            if(ss.length==2){
                try {
                    Date d = sdf.parse(ss[0]);
                    Calendar c = Calendar.getInstance();
                    c.setTime(d);
                    int year = c.get(Calendar.YEAR);
                    String hot = ss[1].substring(0,ss[1].indexOf("℃"));
                    keyPari.setYear(year);
                    keyPari.setHot(Integer.parseInt(hot));
                    context.write(keyPari,value);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}