package org.mdp.hadoop.cli;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

public class ConsecutiveWinnersAnalysis {
    
    public static class ConsecutiveWinnersMapper 
        extends Mapper<Object, Text, Text, Text>{
        
        @Override
        public void map(Object key, Text value, Context context
                       ) throws IOException, InterruptedException {
            
            String[] columns = value.toString().split(",");
            
            
            if(columns.length < 37 || "pos".equals(columns[14].trim())) {
                return;
            }
            
            String position = columns[15].trim();
            String horse = columns[19].trim();
            String date = columns[0].trim();    
            String raceId = columns[1].trim();   
            String age = columns[20].trim();     
            String weight = columns[22].trim();  
            String jockey = columns[26].trim();  
            String trainer = columns[27].trim(); 
            String or_rating = columns[29].trim(); 
            
            
            if("1".equals(position) && !horse.isEmpty()) {
                
                String raceInfo = date + "|" + raceId + "|" + age + "|" + weight + 
                                "|" + jockey + "|" + trainer + "|" + or_rating;
                context.write(new Text(horse), new Text(raceInfo));
            }
        }
    }

    public static class ConsecutiveWinnersReducer 
        extends Reducer<Text, Text, Text, Text> {

        @Override
        public void reduce(Text key, Iterable<Text> values,
                          Context context) throws IOException, InterruptedException {
            
            List<String> races = new ArrayList<>();
            for(Text val : values) {
                races.add(val.toString());
            }
            
            Collections.sort(races);
            
            int consecutiveWins = 1;
            int maxConsecutive = 1;
            String characteristics = "";
            
            if(races.size() >= 3) {
                String firstRace = races.get(0);
                String[] firstParts = firstRace.split("\\|");
                
                if(firstParts.length >= 7) {
                    characteristics = "Age:" + firstParts[2] + 
                                    " AvgWeight:" + calculateAvgWeight(races) +
                                    " MainJockey:" + getMostFrequentJockey(races) +
                                    " MainTrainer:" + getMostFrequentTrainer(races) +
                                    " AvgRating:" + calculateAvgRating(races);
                }
                
                for(int i = 1; i < races.size(); i++) {
                    String[] currentParts = races.get(i).split("\\|");
                    String[] prevParts = races.get(i-1).split("\\|");
                    
                    if(isConsecutiveRace(prevParts[0], currentParts[0])) {
                        consecutiveWins++;
                        maxConsecutive = Math.max(maxConsecutive, consecutiveWins);
                    } else {
                        consecutiveWins = 1;
                    }
                }
                
                
                if(maxConsecutive >= 3) {
                    String result = "MaxConsecutive:" + maxConsecutive + 
                                  " TotalWins:" + races.size() + 
                                  " " + characteristics;
                    context.write(key, new Text(result));
                }
            }
        }
        
        private String calculateAvgWeight(List<String> races) {
            double totalWeight = 0;
            int count = 0;
            for(String race : races) {
                String[] parts = race.split("\\|");
                if(parts.length > 3 && !parts[3].isEmpty()) {
                    try {
                        totalWeight += Double.parseDouble(parts[3]);
                        count++;
                    } catch(NumberFormatException e) {
                    }
                }
            }
            return count > 0 ? String.format("%.1f", totalWeight/count) : "N/A";
        }
        
        private String getMostFrequentJockey(List<String> races) {
            // Simple approach - return first jockey found
            for(String race : races) {
                String[] parts = race.split("\\|");
                if(parts.length > 4 && !parts[4].isEmpty()) {
                    return parts[4];
                }
            }
            return "N/A";
        }
        
        private String getMostFrequentTrainer(List<String> races) {
            for(String race : races) {
                String[] parts = race.split("\\|");
                if(parts.length > 5 && !parts[5].isEmpty()) {
                    return parts[5];
                }
            }
            return "N/A";
        }
        
        private String calculateAvgRating(List<String> races) {
            double totalRating = 0;
            int count = 0;
            for(String race : races) {
                String[] parts = race.split("\\|");
                if(parts.length > 6 && !parts[6].isEmpty()) {
                    try {
                        totalRating += Double.parseDouble(parts[6]);
                        count++;
                    } catch(NumberFormatException e) {
                        // Ignore invalid ratings
                    }
                }
            }
            return count > 0 ? String.format("%.1f", totalRating/count) : "N/A";
        }
        
        private boolean isConsecutiveRace(String date1, String date2) {
            
            
            return !date1.equals(date2);
        }
    }

    public static void main(String[] args) throws Exception {
        Configuration conf = new Configuration();
        String[] otherArgs = new GenericOptionsParser(conf, args).getRemainingArgs();
        if (otherArgs.length != 2) {
            System.err.println("Usage: ConsecutiveWinnersAnalysis <input> <output>");
            System.exit(2);
        }
        
        Job job = Job.getInstance(conf, "Consecutive Winners Analysis");
        job.setJarByClass(ConsecutiveWinnersAnalysis.class);
        
        job.setMapperClass(ConsecutiveWinnersMapper.class);
        job.setReducerClass(ConsecutiveWinnersReducer.class);
        
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);
        
        FileInputFormat.addInputPath(job, new Path(otherArgs[0]));
        FileOutputFormat.setOutputPath(job, new Path(otherArgs[1]));
        
        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }    
}