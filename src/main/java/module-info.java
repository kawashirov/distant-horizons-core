module dhApi {
	requires org.apache.logging.log4j;
	requires org.lwjgl.opengl;
	requires java.datatransfer;
	requires java.desktop;
	requires java.sql;
	requires org.lwjgl.glfw;
	requires com.formdev.flatlaf;
	requires com.formdev.flatlaf.extras;
	requires com.google.common;
	requires json.simple;
	requires core; // electronwill.nightconfig

	// annotations
	requires org.jetbrains.annotations;
	requires jsr305;


    // Distant Horizons' API exports
	exports com.seibel.lod.core.api.external;
}
