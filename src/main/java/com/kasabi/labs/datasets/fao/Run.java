package com.kasabi.labs.datasets.fao;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import com.kasabi.labs.datasets.Utils;

public class Run {

	public static final File[] data = {
		new File("data/fao", "fao-data.rdf"),
	};
	
	public static void main(String[] args) throws IOException {
		File query = new File("data/queries/", "gdp.sparql") ;
		FileWriter writer = new FileWriter("gdp.html");
		Utils.render(data, query, writer) ;
		writer.flush() ;
		writer.close() ;
	}

}
