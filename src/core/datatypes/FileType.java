package core.datatypes;

public enum FileType {
	Audio("mp3", "ogg", "m4a", "flac", "wav"),
	Video("mp4", "mpeg", "wmv", "avi", "mkv", "divx", "xvid", "flv"),
	Image("png", "jpg", "jpeg", "gif", "bmp", "apng", "tiff", "raw"),
	Executable("exe", "sh", "bat", "bin"),
	Compressed("zip", "rar", "pak", "ue3", "game", "7z", "tar", "gz", "bzip", "smc", "nes"),
	Other();
	
	private String[] types;
	
	private FileType(String... types)
	{
		this.types = types;
	}
	
	public static FileType getType(String s)
	{
		if (s == null)
			return Other;
		
		for (FileType t : values())
		{
			if (s.toLowerCase().equals(t.toString()))
				return t;
			
			for (String type : t.types)
			{
				if (type.equals(s))
					return t;
			}
		}
		return Other;
	}
	
	public String toString()
	{
		return this.name().toLowerCase();
	}
}
