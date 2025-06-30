package org.mdp.hadoop.cli;

import java.io.IOException;
import java.util.TreeMap;

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

public class TopRacedHorses {

    public static class RaceCountMapper 
        extends Mapper<Object, Text, Text, IntWritable> {
        
        private final static IntWritable one = new IntWritable(1);
        private Text horseName = new Text();
        
        @Override
        public void map(Object key, Text value, Context context
                      ) throws IOException, InterruptedException {
            
            String[] columns = value.toString().split(",", -1);
            
            if(columns.length < 20 || "horse".equals(columns[19].trim())) {
                return;
            }
            
            String horse = columns[19].trim();
            
            if(!horse.isEmpty()) {
                horseName.set(horse);
                context.write(horseName, one);
            }
        }
    }

    public static class TopHorsesReducer 
        extends Reducer<Text, IntWritable, Text, IntWritable> {
        
        private TreeMap<Integer, Text> topHorses = new TreeMap<>();
        
        @Override
        public void reduce(Text key, Iterable<IntWritable> values,
                          Context context) throws IOException, InterruptedException {
            
            int raceCount = 0;
            for(IntWritable val : values) {
                raceCount += val.get();
            }
            
            // Add to our top list (limit to 10)
            topHorses.put(raceCount, new Text(key));
            if(topHorses.size() > 10) {
                topHorses.remove(topHorses.firstKey());
            }
        }
        
        @Override
        protected void cleanup(Context context) 
            throws IOException, InterruptedException {
            
            context.write(new Text("Top 10 most raced horses:"), null);
            
            // Emit in descending order
            for(int count : topHorses.descendingKeySet()) {
                Text horse = topHorses.get(count);
                context.write(horse, new IntWritable(count));
            }
        }
    }

    public static void main(String[] args) throws Exception {
        Configuration conf = new Configuration();
        String[] otherArgs = new GenericOptionsParser(conf, args).getRemainingArgs();
        if (otherArgs.length != 2) {
            System.err.println("Usage: TopRacedHorses <input> <output>");
            System.exit(2);
        }
        
        Job job = Job.getInstance(conf, "Top Raced Horses");
        job.setJarByClass(TopRacedHorses.class);
        
        job.setMapperClass(RaceCountMapper.class);
        job.setReducerClass(TopHorsesReducer.class);
        
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(IntWritable.class);
        
        job.setNumReduceTasks(1);
        
        FileInputFormat.addInputPath(job, new Path(otherArgs[0]));
        FileOutputFormat.setOutputPath(job, new Path(otherArgs[1]));
        
        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
}