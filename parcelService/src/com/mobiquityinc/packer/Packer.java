package com.mobiquityinc.packer;

import com.mobiquityinc.exception.APIException;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class Packer {

    public static void main(String[] args) {
        String pathToTestFile = "./input.txt";
        System.out.println(pack(pathToTestFile));
    }

    private static String pack(String pathToTestFile) {
        BufferedReader br = null;
        FileReader fr = null;
        StringBuilder totalResult = new StringBuilder();
        processInputFile(pathToTestFile, br, fr, totalResult);
        return totalResult.toString();
    }

    private static void processInputFile(String pathToTestFile, BufferedReader br, FileReader fr, StringBuilder totalResult) {
        try {

            fr = new FileReader(pathToTestFile);
            br = new BufferedReader(fr);

            String sCurrentLine;

            // Read the file line by line
            for (int i = 0; (sCurrentLine = br.readLine()) != null; i++) {
                // create lists and limit param holding the parsed line details
                int weightLimit = 0;
                List<Integer> indecesList = new ArrayList<>();
                List<Double> weightsList = new ArrayList<>();
                // add one extra entry to be inline while fetching corresponding values from index
                weightsList.add(0.0);
                List<Integer> valuesList = new ArrayList<>();
                // add one extra entry to be inline while fetching corresponding values from index
                valuesList.add(0);

                StringTokenizer currentLine = new StringTokenizer(sCurrentLine, " ");
                int countelements = 0;
                for (int segment = 0; currentLine.hasMoreElements(); segment++) {
                    if (segment == 0) {
                        weightLimit = Integer.parseInt(currentLine.nextElement().toString());
                        if (weightLimit <= 0 || weightLimit > 100) {
                            throw new APIException("Invalid weight Limit given at line " + (i + 1) + " weightLimit " + weightLimit);
                        }
                    } else if (segment == 1) {
                        // to neglect :
                        currentLine.nextElement();
                    } else {
                        String readEachElementDetails = currentLine.nextElement().toString();
                        if (readEachElementDetails != null || !readEachElementDetails.isEmpty()) {
                            countelements++;
                        }
                        // eliminate ( and )
                        readEachElementDetails = readEachElementDetails.substring(1, readEachElementDetails.length() - 1);
                        StringTokenizer strDetails = new StringTokenizer(readEachElementDetails, ",");
                        for (int subSegment = 0; strDetails.hasMoreElements(); subSegment++) {
                            if (subSegment == 0) {
                                int index = Integer.parseInt(strDetails.nextElement().toString());
                                indecesList.add(index);
                                if (index > 15 || index <= 0) {
                                    throw new APIException(" Invalid index number specified in line " + (i + 1) + " segment " + (segment + 1) +
                                            " subSegment " + (subSegment + 1) + " index " + index);
                                }
                            } else if (subSegment == 1) {
                                double weight = Double.parseDouble(strDetails.nextElement().toString());
                                weightsList.add(weight);
                                if (weight < 0 || weight > 100) {
                                    throw new APIException(" Invalid weight of item specified in line " + (i + 1) + " segment " + (segment + 1) +
                                            " subSegment " + (subSegment + 1) + " weight " + weight);
                                }
                            } else {
                                String valueWithCurrency = strDetails.nextElement().toString();
                                // remove € before conversion
                                int value = Integer.parseInt(valueWithCurrency.substring(1, valueWithCurrency.length()));
                                valuesList.add(value);
                                if (value < 0 || value > 100) {
                                    throw new APIException(" Invalid value of item specified in line " + (i + 1) + " segment " + (segment + 1) +
                                            " subSegment " + (subSegment + 1) + " cost " + value);
                                }
                            }

                        }
                    }
                }
                if (countelements == 0 || countelements > 15) {
                    throw new APIException(" Invalid number of elements specified in line " + (i + 1) + " elements count " + countelements);
                }

                double[] weightArray = new double[weightsList.size()];
                int[] valueArray = new int[valuesList.size()];
                for (int ind = 0; ind < indecesList.size() + 1; ind++) {
                    weightArray[ind] = weightsList.get(ind);
                    valueArray[ind] = valuesList.get(ind);
                }
                totalResult.append("\n").append(packResult(weightArray, valueArray, weightLimit, indecesList.size()));
            }

        } catch (IOException e) {
            System.out.println("IO Exception occurred while parsing input file " + pathToTestFile + " " + e.getMessage());
        } catch (NumberFormatException numberFmt) {
            System.out.println("String passed when number expected " + numberFmt.getMessage());
        } catch (APIException apiEx) {
            System.out.println("Invalid parameters passed " + apiEx.getMessage());
        } finally {
            try {
                if (br != null)
                    br.close();
                if (fr != null)
                    fr.close();
            } catch (IOException ex) {
                System.out.println("IO Exception occurred while closing input file " + pathToTestFile + " " + ex.getMessage());
            }

        }
    }


    public static String packResult(double[] weight, int[] value, int weightLimit, int N) {
        int NEGATIVE_VALUE = Integer.MIN_VALUE;
        int[][] cumulativeValue = new int[N + 1][weightLimit + 1];
        int[][] markSelection = new int[N + 1][weightLimit + 1];

        for (int i = 1; i <= N; i++) {
            for (int j = 0; j <= weightLimit; j++) {
                int previousCalCumulativeValue = cumulativeValue[i - 1][j];
                int currentCalCumulativeValue = NEGATIVE_VALUE;
                /** check if current item row(i) index weight(j) is  greater or equal to current item weight **/
                if (j >= weight[i])
                /** if yes current calculated value which is current item value +  value in previous item row
                 *  with index which is difference between current item row index to current item weight  **/
                /** we are ceiling the double value of weight difference to next valid int array index  **/
                    currentCalCumulativeValue = value[i] + cumulativeValue[i - 1][(int) Math.ceil(j - weight[i])];
                /** select max of previous item cal cumulative value and  current item cal cumulative value **/
                /** Note : This becomes previous item cal cumulative value for next item
                 * so store this value in current item row index **/
                cumulativeValue[i][j] = Math.max(previousCalCumulativeValue, currentCalCumulativeValue);
                // mark the selection change if current cal cumulative values are dominant
                markSelection[i][j] = currentCalCumulativeValue > previousCalCumulativeValue ? 1 : 0;
                /** if currentCumulative value is equal to previous cumulative value we over rule the marking only
                 if weight of current item is less than or equal to previous item **/
                if (currentCalCumulativeValue == previousCalCumulativeValue && weight[i] <= weight[i - 1]) {
                    markSelection[i][j] = 1;
                    markSelection[i - 1][j] = 0;
                }
            }
        }
        /** make list of what all items to finally select **/
        int[] selected = new int[N + 1];
        for (int n = N, w = weightLimit; n > 0; n--) {
            // start with last row item , index
            // check if it is marked
            if (markSelection[n][w] != 0) {
                // if yes then this item row is part of the final result
                // FYI : the value in cumulativeValue[N][weightLimit] is the value of minimum possible weights gathered
                selected[n] = 1;
                // now reduce weight removing this item weight
                /** we are ceiling the double value of weight difference to next valid int array index  **/
                w = (int) Math.ceil(w - weight[n]);
            } else
                // if no , this item is not part of final selection
                selected[n] = 0;
        }
        /** Print finally selected items **/
        String result = "";
        for (int i = 1; i < N + 1; i++) {
            if (selected[i] == 1) {
                result = result + i + ",";
            }
        }
        result = result.isEmpty() ? "-" : result.substring(0, result.length() - 1);

        return result;
    }
}
