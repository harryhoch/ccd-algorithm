package edu.pitt.dbmi.ccd.algorithm.tetrad.data;

import edu.cmu.tetrad.data.ColtDataSet;
import edu.cmu.tetrad.data.ContinuousVariable;
import edu.cmu.tetrad.data.DataReader;
import edu.cmu.tetrad.data.DataSet;
import edu.cmu.tetrad.data.DataWriter;
import edu.cmu.tetrad.data.DelimiterType;
import edu.cmu.tetrad.data.DiscreteVariable;
import edu.cmu.tetrad.graph.Node;
import edu.cmu.tetrad.util.TetradMatrix;
import edu.pitt.dbmi.ccd.algorithm.util.FileTools;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

/**
 * Class that handles reading and writing dataset.
 *
 * Feb 13, 2015 1:52:04 PM
 *
 * @author Kevin V. Bui (kvb2@pitt.edu)
 */
public class DataSetIO {

    public static final char TAB_DELIMITER = '\t';
    public static final char SPACE_DELIMITER = ' ';
    public static final char COMMA_DELIMITER = ',';
    public static final char COLON_DELIMITER = ':';

    public static final byte NEW_LINE = '\n';
    public static final byte SPACE = ' ';
    public static final byte TAB = '\t';

    public static void write(DataSet dataSet, char delimiter, File file) throws IOException {
        Path path = file.toPath();
        try (BufferedWriter writer = Files.newBufferedWriter(path)) {
            DataWriter.writeRectangularData(dataSet, writer, delimiter);
        }
    }

    public static DataSet read(char delimiter, File file) throws IOException {
        DataReader dataReader = new DataReader();
        switch (delimiter) {
            case TAB_DELIMITER:
                dataReader.setDelimiter(DelimiterType.TAB);
                break;
            case COLON_DELIMITER:
                dataReader.setDelimiter(DelimiterType.COLON);
                break;
            case COMMA_DELIMITER:
                dataReader.setDelimiter(DelimiterType.COMMA);
                break;
            default:
                dataReader.setDelimiter(DelimiterType.WHITESPACE);
                break;
        }
        return dataReader.parseTabular(file);
    }

    public static DataSet read(char delimiter, File file, boolean continuous) throws IOException {
        byte delim = (byte) delimiter;
        List<Node> nodes = readInNodes(file, delim, continuous);
        TetradMatrix tetradMatrix = readInTetradMatrix(file, delim);

        return new ColtDataSet(nodes, tetradMatrix);
    }

    private static TetradMatrix readInTetradMatrix(File file, byte delimiter) throws IOException {
        return new TetradMatrix(readInData(file, delimiter));
    }

    private static double[][] readInData(File file, byte delimiter) throws IOException {
        int numRow = FileTools.countLine(file) - 1;
        int numCol = FileTools.countColumn(file, delimiter);

        double[][] data = new double[numRow][numCol];

        try (FileChannel fc = new RandomAccessFile(file, "r").getChannel()) {
            MappedByteBuffer buffer = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());

            // skip the header
            while (buffer.hasRemaining()) {
                if (buffer.get() == NEW_LINE) {
                    break;
                }
            }

            // parse data
            StringBuilder dataBuilder = new StringBuilder();
            int row = 0;
            int col = 0;
            while (buffer.hasRemaining()) {
                byte currentChar = buffer.get();
                if (currentChar == delimiter || currentChar == NEW_LINE) {
                    data[row][col++] = Double.parseDouble(dataBuilder.toString());
                    dataBuilder.delete(0, dataBuilder.length());
                    if (currentChar == NEW_LINE) {
                        col = 0;
                        row++;
                    }
                } else {
                    if (!(currentChar == SPACE || currentChar == TAB)) {
                        dataBuilder.append((char) currentChar);
                    }
                }
            }
        }

        return data;
    }

    private static List<Node> readInNodes(File file, byte delimiter, boolean continuous) throws IOException {
        List<Node> vars = new LinkedList<>();

        try (FileChannel fc = new RandomAccessFile(file, "r").getChannel()) {
            MappedByteBuffer buffer = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
            StringBuilder dataBuilder = new StringBuilder();
            while (buffer.hasRemaining()) {
                byte currentChar = buffer.get();
                if (currentChar == delimiter || currentChar == NEW_LINE) {
                    String value = dataBuilder.toString();
                    if (continuous) {
                        vars.add(new ContinuousVariable(value));
                    } else {
                        vars.add(new DiscreteVariable(value));
                    }
                    dataBuilder.delete(0, dataBuilder.length());
                    if (currentChar == NEW_LINE) {
                        break;
                    }
                } else {
                    dataBuilder.append((char) currentChar);
                }
            }
        }

        return vars;
    }

}
