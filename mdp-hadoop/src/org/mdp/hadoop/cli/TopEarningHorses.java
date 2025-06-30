package org.mdp.hadoop.cli;

import java.io.IOException;
import java.util.PriorityQueue;
import java.util.regex.Pattern;
import java.text.NumberFormat;
import java.util.Locale;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;

public class TopEarningHorses {

   
    private static final double EURO_TO_CLP = 1091.0;
    private static final double GBP_TO_CLP = 1278.0;
    
    public static class PrizeMapper 
        extends Mapper<Object, Text, Text, LongWritable> {
        
        private Text horseName = new Text();
        private LongWritable prizeCLP = new LongWritable();
        private final Pattern COMMA_PATTERN = Pattern.compile(",");
        
        @Override
        public void map(Object key, Text value, Context context)
                      throws IOException, InterruptedException {
            
            String[] columns = value.toString().split(",", -1);
            
           
            if(columns.length < 29 || "prize".equals(columns[28].trim())) {
                return;
            }
            
            String horse = columns[19].trim();
            String prizeStr = columns[28].trim();
            
           
            if(!horse.isEmpty() && !prizeStr.isEmpty()) {
                long prizeValue = convertPrizeToCLP(prizeStr);
                horseName.set(horse);
                prizeCLP.set(prizeValue);
                context.write(horseName, prizeCLP);
            }
        }
        
        private long convertPrizeToCLP(String prizeStr) {
            try {
               
                String cleanStr = prizeStr.replaceAll("[^\\d.£€]", "");
                
               
                boolean isEuro = cleanStr.contains("€");
                boolean isGBP = cleanStr.contains("£") || (!isEuro && !cleanStr.isEmpty());
                
               
                String numericStr = cleanStr.replace("€", "").replace("£", "");
                
               
                NumberFormat format = NumberFormat.getInstance(Locale.US);
                double amount = format.parse(numericStr).doubleValue();
                
             
                if(isEuro) {
                    return (long) (amount * EURO_TO_CLP);
                } else if(isGBP) {
                    return (long) (amount * GBP_TO_CLP);
                }
            } catch (Exception e) {
                
            }
            return 0;
        }
    }

    public static class TopEarnersReducer 
        extends Reducer<Text, LongWritable, Text, LongWritable> {
        
        
        static class HorseEarnings {
            String name;
            long totalEarnings;
            
            HorseEarnings(String name, long earnings) {
                this.name = name;
                this.totalEarnings = earnings;
            }
        }
        
       
        private PriorityQueue<HorseEarnings> topHorses = new PriorityQueue<>(
            (a, b) -> Long.compare(a.totalEarnings, b.totalEarnings)
        );
        
        @Override
        public void reduce(Text key, Iterable<LongWritable> values,
                          Context context) throws IOException, InterruptedException {
            
            long totalEarnings = 0;
            for(LongWritable val : values) {
                totalEarnings += val.get();
            }
            
          
            topHorses.add(new HorseEarnings(key.toString(), totalEarnings));
            if(topHorses.size() > 10) {
                topHorses.poll(); // Remueve el caballo con menos ganancias
            }
        }
        
        @Override
        protected void cleanup(Context context) 
            throws IOException, InterruptedException {
            
            context.write(new Text("Top 10 Caballos con más ganancias (CLP):"), null);
            
            
            while(!topHorses.isEmpty()) {
                HorseEarnings horse = topHorses.poll();
                context.write(new Text(horse.name), new LongWritable(horse.totalEarnings));
            }
        }
    }

    public static void main(String[] args) throws Exception {
        Configuration conf = new Configuration();
        String[] otherArgs = new GenericOptionsParser(conf, args).getRemainingArgs();
        if (otherArgs.length != 2) {
            System.err.println("Usage: TopEarningHorses <input> <output>");
            System.exit(2);
        }
        
        Job job = Job.getInstance(conf, "Top Earning Horses");
        job.setJarByClass(TopEarningHorses.class);
        
        job.setMapperClass(PrizeMapper.class);
        job.setReducerClass(TopEarnersReducer.class);
        
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(LongWritable.class);
        
       
        job.setNumReduceTasks(1);
        
        FileInputFormat.addInputPath(job, new Path(otherArgs[0]));
        FileOutputFormat.setOutputPath(job, new Path(otherArgs[1]));
        
        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
}