/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kasabi.labs.datasets;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFactory;
import com.hp.hpl.jena.query.ResultSetRewindable;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.util.FileManager;

public class Utils {

	private static final Logger log = LoggerFactory.getLogger(Utils.class) ;
	
	// loads some files and run a query
	public static ResultSet select ( File[] load, File query ) {
		Model model = load(load);
		Query q = parse ( query ) ;
		QueryExecution qexec = QueryExecutionFactory.create(q, model);
		ResultSetRewindable results = null;
		try {
			results = ResultSetFactory.makeRewindable(qexec.execSelect());
		} finally {
			qexec.close();
		}
		return results;
	}
	
	public static void render ( File[] load, File query, Writer writer ) {
		ResultSet results = select ( load, query ) ;
		VelocityEngine ve = new VelocityEngine();
		ve.setProperty("file.resource.loader.path", query.getParentFile().getAbsolutePath()) ;
		ve.init();
		Template t = ve.getTemplate(extension ( query.getName(), "template" ) );
		VelocityContext context = new VelocityContext();
		context.put("resultset", results);
		context.put("formatter", new String());
		t.merge(context, writer);
		
	}

	public static Model construct ( File[] load, File query ) {
		return construct ( load, new File[]{}, new File[]{}, query ) ;
	}

	public static Model construct ( File[] load, File[] merge, File query ) {
		return construct ( load, merge, new File[]{}, query ) ;
	}

	// loads some files, run a construct query and merge more data with the result
	public static Model construct ( File[] load, File[] merge, File[] remove, File query ) {
		Model model = load ( load ) ;
		Query q = parse ( query ) ;
		QueryExecution qexec = QueryExecutionFactory.create(q, model) ;
		Model result = null ;
		try {
			result = qexec.execConstruct() ;
			remove ( result, remove ) ;
			merge ( result, merge ) ;
		} finally {
			qexec.close() ;
		}
		return result ;
	}

	public static Model load ( File[] files ) {
		Model model = ModelFactory.createDefaultModel() ;
		for ( File file : files ) {
			log.debug("Loading {} ...", file.getAbsolutePath()) ;
			long start = System.currentTimeMillis() ;
			FileManager.get().readModel(model, file.getAbsolutePath()) ;
			long stop = System.currentTimeMillis() ;
			log.debug("Loaded {} triples in {} ms.", model.size(), (stop-start));
		}
		return model ;
	}

	public static void merge ( Model model, File[] files ) {
		for ( File file : files ) {
			FileManager.get().readModel(model, file.getAbsolutePath()) ;
		}
	}

	public static void remove ( Model model, File[] files ) {
		for ( File file : files ) {
			Model m = FileManager.get().loadModel(file.getAbsolutePath()) ;
			model.remove(m) ;
		}
	}

	public static Query parse ( File query ) {
		try {
			if ( query.getName().endsWith(".sparql") ) {
				File template = new File (query.getParentFile(), "sparql.vm") ;
				File output = new File (query.getParentFile(), extension ( query.getName(), "rq") ) ;
				VelocityEngine ve = new VelocityEngine();
				ve.setProperty("file.resource.loader.path", query.getParentFile().getAbsolutePath()) ;
				ve.init();
				Template t = ve.getTemplate(template.getName());
				VelocityContext context = new VelocityContext();
				context.put("query", query.getName());
				FileWriter writer = new FileWriter(output);
				t.merge(context, writer);
				writer.flush();
				writer.close();
				return QueryFactory.read(output.getAbsolutePath());
			} else {
				return QueryFactory.read(query.getAbsolutePath());
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static String extension ( String filename, String extension ) {
		String result = null ;

		int index = filename.lastIndexOf(".") ;
		if ( index > 0 ) {
			result = filename.substring(0, index + 1) + extension ;
		}

		return result ;
	}

}
