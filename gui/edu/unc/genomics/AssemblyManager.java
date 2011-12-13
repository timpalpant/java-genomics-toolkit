package edu.unc.genomics;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.DataFormatException;

import org.apache.log4j.Logger;

public class AssemblyManager {
	
	private static final Logger log = Logger.getLogger(AssemblyManager.class);
	
	/**
	 * The last used Assembly
	 */
	private static Assembly lastUsed;
	
	/**
	 * Returns all available assemblies in the resources directory
	 * @return the assemblies available in the resources directory
	 */
	public static List<Assembly> getAvailableAssemblies() {
		List<Assembly> assemblies = new ArrayList<>();
		
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(AssemblyConverter.ASSEMBLIES_DIR, "*.{len}")) {
      for (Path entry : stream) {
      	log.debug("Loading assembly: " + entry);
				try {
					Assembly a = new Assembly(entry);
					assemblies.add(a);
				} catch (IOException | DataFormatException e1) { 
					log.warn("Error loading assembly: " + entry);
				}
      }
		} catch (IOException e) {
			log.error("Error listing assemblies");
			e.printStackTrace();
		}
		
		return assemblies;
	}
	
	public static void deleteAssembly(Assembly a) throws IOException {
		Files.deleteIfExists(a.getPath());
	}
	
	public static Assembly loadCustomAssembly(Path assemblyFile) throws IOException, DataFormatException {
		log.debug("Loading custom assembly from file: " + assemblyFile);
		Assembly a = new Assembly(assemblyFile);
		
		// TODO: Warn if this assembly is already loaded
		
		// Copy the assembly file into the built-in assemblies directory
		Files.copy(assemblyFile, AssemblyConverter.ASSEMBLIES_DIR.resolve(assemblyFile.getFileName()));
		return a;
	}

	/**
	 * @return the lastUsed
	 */
	public static Assembly getLastUsed() {
		return lastUsed;
	}

	/**
	 * @param lastUsed the lastUsed to set
	 */
	public static void setLastUsed(Assembly lastUsed) {
		AssemblyManager.lastUsed = lastUsed;
	}
}
