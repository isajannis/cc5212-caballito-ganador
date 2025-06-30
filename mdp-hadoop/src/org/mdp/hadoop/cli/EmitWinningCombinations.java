package org.mdp.hadoop.cli;

import java.io.IOException;

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

public class EmitWinningCombinations {
    
    public static class WinningCombinationsMapper 
        extends Mapper<Object, Text, Text, IntWritable>{
        
        private final static IntWritable one = new IntWritable(1);
        
        @Override
        public void map(Object key, Text value, Context context
                       ) throws IOException, InterruptedException {
            
            String[] columns = value.toString().split(",");
            
            
            if(columns.length < 37 || "pos".equals(columns[14].trim())) {
                return;
            }
            
            String position = columns[15].trim();
            String horse = columns[19].trim();    
            String jockey = columns[26].trim();  
            String trainer = columns[27].trim();  
            
            
            if("1".equals(position)) {
                
                if(!horse.isEmpty() && !jockey.isEmpty()) {
                    String comboKey = "HORSE_JOCKEY:" + horse + " - " + jockey;
                    context.write(new Text(comboKey), one);
                }
                
                
                if(!trainer.isEmpty() && !jockey.isEmpty()) {
                    String comboKey = "TRAINER_JOCKEY:" + trainer + " - " + jockey;
                    context.write(new Text(comboKey), one);
                }
            }
        }
    }

    public static class WinningCombinationsReducer 
        extends Reducer<Text, IntWritable, Text, IntWritable> {

        @Override
        public void reduce(Text key, Iterable<IntWritable> values,
                          Context context) throws IOException, InterruptedException {
            
            int winCount = 0;
            for(IntWritable val : values) {
                winCount += val.get();
            }
            context.write(key, new IntWritable(winCount));
        }
    }

    public static void main(String[] args) throws Exception {
        Configuration conf = new Configuration();
        String[] otherArgs = new GenericOptionsParser(conf, args).getRemainingArgs();
        if (otherArgs.length != 2) {
            System.err.println("Usage: EmitWinningCombinations <input> <output>");
            System.exit(2);
        }
        
        Job job = Job.getInstance(conf, "Winning Combinations");
        job.setJarByClass(EmitWinningCombinations.class);
        
        job.setMapperClass(WinningCombinationsMapper.class);
        job.setCombinerClass(WinningCombinationsReducer.class);
        job.setReducerClass(WinningCombinationsReducer.class);
        
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(IntWritable.class);
        
        FileInputFormat.addInputPath(job, new Path(otherArgs[0]));
        FileOutputFormat.setOutputPath(job, new Path(otherArgs[1]));
        
        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }    
}