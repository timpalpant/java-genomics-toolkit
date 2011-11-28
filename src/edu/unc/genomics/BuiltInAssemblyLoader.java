package edu.unc.genomics;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.DataFormatException;

public class BuiltInAssemblyLoader {
	
	public static final Path ASSEMBLIES_DIR = Paths.get("resources", "assemblies");
	
	/**
	 * Look for a built-in assembly in the resources dir,
	 * else assume it is a filename
	 * @param name
	 * @return
	 * @throws DataFormatException 
	 * @throws IOException 
	 */
	public static Assembly loadAssembly(String name) throws IOException, DataFormatException {
		Path p = ASSEMBLIES_DIR.resolve(name+".len");
		if (Files.isReadable(p)) {
			return new Assembly(p);
		}
		
		return new Assembly(Paths.get(name));
	}
}
