import java.util.List;
import java.util.LinkedList;
import java.util.Arrays;
import java.util.Iterator;
import java.io.PrintWriter;
import java.io.FileNotFoundException;
import java.io.File;
import java.io.FileFilter;

import edu.stanford.nlp.parser.lexparser.*;
import edu.stanford.nlp.parser.lexparser.Options.LexOptions;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.trees.*;
import edu.stanford.nlp.ling.*;

public class BrownPreprocessor
{
    public static void main(String[] args)
    {
	BrownPreprocessor bp = new BrownPreprocessor();
	String path = args[0];

	// Read in genres separately.
	List<Treebank> genreBanks = bp.parseGenres(path);
	
	// Arbitrary train/test division.
	double trainPercentage = 0.90;
	
	// Build full train and test sets over all genres.
	Treebank trainFull = bp.generateTreebank();
	Treebank testFull  = bp.generateTreebank();
	for (Treebank t : genreBanks)
	{
	    int trainSize = (int)(t.size() * trainPercentage);
	    Iterator<Tree> it = t.iterator();
	    int index = 0;
	    while (it.hasNext())
	    {
		Tree next = it.next();
		if (index < trainSize)
		    trainFull.add(next);
		else
		    testFull.add(next);
			       
		index += 1;
	    }
	}
	
	String out;

	// Generate Brown self-training sets.
	int[] trainSizes = {1000, 2000, 3000, 4000, 5000, 7000, 10000, 13000, 17000, 21000};
	for (int trainSize : trainSizes)
	{
	    out = String.format("browntraining%d.txt", trainSize);
	    bp.buildTrainSet(trainFull, trainSize, out);
	    bp.confirmSize(out, trainSize);
	    System.out.println("Build training set of size " + trainSize + ".");
	}

	// Generate Brown entire test corpus.
	System.out.println("Building Brown test corpus for testing.");
	out = String.format("browntest.txt");
	bp.buildTrainSet(testFull, testFull.size(), out);
	
	// Generate Brown entire train corpus.
	System.out.println("Building Brown entire train corpus for seed set.");
	out = String.format("browntrainentire.txt");
	bp.buildTrainSet(trainFull, trainFull.size(), out);
    }

    public List<Treebank> parseGenres(String brownPath)
    {
	// Brown corpus should be a directory with sub-genre directories.
	File brownDirectory = new File(brownPath);
	assert (brownDirectory.isDirectory());
	
	// Parse genres.
	List<Treebank> genreBanks = new LinkedList<Treebank>();
	for (File file : brownDirectory.listFiles())
	{
	    Treebank newGenre = this.generateTreebank();
	    newGenre.loadPath(file.getAbsolutePath());
	    genreBanks.add(newGenre);
	} 
	
	return genreBanks;
    }

    public void buildTrainSet(Treebank fullData, int trainSentences, String out)
    {
	// Build train set of specified size.
	Treebank train = generateTreebank();
	Iterator<Tree> it = fullData.iterator();
	int index = 0;
	while (it.hasNext() && index < trainSentences)
	{
	    Tree next = it.next();
	    train.add(next);
	    index += 1;
	}
	
	writeToFile(train, out);
    }

    public void confirmSize(String path, int size)
    {
	Treebank confirm = this.generateTreebank();
	confirm.loadPath(path);
	assert (confirm.size() == size);	
    }

    public Treebank generateTreebank()
    {
	// Load as Treebank for easy separation.
	Options options = new Options();
	options.doDep = false;
	options.doPCFG = true;
	options.setOptions("-goodPCFG", "-evals", "tsv");
	return options.tlpParams.testMemoryTreebank();
    }
    
    public void writeToFile(Treebank treebank, String filename)
    {
	TreePrint printer = new TreePrint("penn");
	try (PrintWriter out = new PrintWriter( filename ))
	{
	    for (Tree t : treebank)
	    {
		printer.printTree(t, out);
	    }
	}
	catch (FileNotFoundException exc)
	{
	    System.out.println("FileNotFoundException.");
	}	
    }
}
