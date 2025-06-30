package org.mdp.hadoop.cli;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;

public class MostProgenitiveSires {

    public static class SireMapper 
        extends Mapper<Object, Text, Text, Text> {
        
        private Text sireName = new Text();
        private Text horseName = new Text();
        
        @Override
        public void map(Object key, Text value, Context context
                      ) throws IOException, InterruptedException {
            
            String[] columns = value.toString().split(",", -1);
            
            
            if(columns.length < 33 || "sire".equals(columns[32].trim())) {
                return;
            }
            
            String horse = columns[19].trim();
            String sire = columns[32].trim(); 
            
            if(!sire.isEmpty() && !horse.isEmpty()) {
                sireName.set(sire);
                horseName.set(horse);
                context.write(sireName, horseName);
            }
        }
    }

    public static class SireReducer 
        extends Reducer<Text, Text, Text, IntWritable> {
        
        
        static class SireCount {
            String name;
            int childCount;
            
            SireCount(String name, int childCount) {
                this.name = name;
                this.childCount = childCount;
            }
        }
        
        
        private PriorityQueue<SireCount> topSires = new PriorityQueue<>(
            (a, b) -> Integer.compare(a.childCount, b.childCount)
        );
        
        @Override
        public void reduce(Text key, Iterable<Text> values,
                          Context context) throws IOException, InterruptedException {
            
            
            Map<String, Boolean> uniqueHorses = new HashMap<>();
            	 
            for(Text horse : values) {
                uniqueHorses.put(horse.toString(), true);
            }
            
            int childCount = uniqueHorses.size();
            
            
            topSires.add(new SireCount(key.toString(), childCount));
            if(topSires.size() > 10) {
                topSires.poll(); // Remueve el elemento con menos hijos
            }
        }
        
        @Override
        protected void cleanup(Context context) 
            throws IOException, InterruptedException {
            
            context.write(new Text("Top 10 most prolific sires:"), null);
            
            
            while(!topSires.isEmpty()) {
                SireCount sire = topSires.poll();
                context.write(new Text(sire.name), new IntWritable(sire.childCount));
            }
        }
    }

    public static void main(String[] args) throws Exception {
        Configuration conf = new Configuration();
        String[] otherArgs = new GenericOptionsParser(conf, args).getRemainingArgs();
        if (otherArgs.length != 2) {
            System.err.println("Usage: MostProgenitiveSires <input> <output>");
            System.exit(2);
        }
        
        Job job = Job.getInstance(conf, "Most Prolific Sires");
        job.setJarByClass(MostProgenitiveSires.class);
        
        job.setMapperClass(SireMapper.class);
        job.setReducerClass(SireReducer.class);
        
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);
        
        
        job.setNumReduceTasks(1);
        
        FileInputFormat.addInputPath(job, new Path(otherArgs[0]));
        FileOutputFormat.setOutputPath(job, new Path(otherArgs[1]));
        
        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
}