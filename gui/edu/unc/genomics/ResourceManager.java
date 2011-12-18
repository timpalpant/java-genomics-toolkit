package edu.unc.genomics;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Find resources based on platform/package
 * @author timpalpant
 *
 */
public class ResourceManager {
	
	private static final Path ASSEMBLIES_DIR = Paths.get("assemblies");
	private static final Path IMAGES_DIR = Paths.get("images");
	
	public static Path getResourceDirectory() {
		Path osx = Paths.get("../Resources");
		if (Files.exists(osx)) {
			return osx;
		}
		
		return Paths.get("resources");
	}
	
	public static Path getAssembliesDirectory() {
		return getResourceDirectory().resolve(ASSEMBLIES_DIR);
	}
	
	public static Path getImagesDirectory() {
		return getResourceDirectory().resolve(IMAGES_DIR);
	}
}
