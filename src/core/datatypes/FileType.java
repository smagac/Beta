package core.datatypes;

public enum FileType {
	Audio(1000, 3000, 10000, 25000, "mp3", "ogg", "m4a", "flac", "wav", "mid"),
	Video(50000, 500000, 1000000, 3500000, "mp4", "mpeg", "wmv", "avi", "mkv", "divx", "xvid", "flv", "vob"),
	Image(500, 1000, 7500, 50000, "png", "jpg", "jpeg", "gif", "bmp", "apng", "tiff", "raw", "xcf"),
	Executable(500, 5000, 100000, 2500000, "exe", "sh", "bat", "bin", "jar"),
	Compressed(100, 1500, 75000, 500000, "zip", "rar", "pak", "ue3", "game", "7z", "tar", "gz", "bzip", "smc", "nes"),
	Other(500, 5000, 1000000, 5000000);
	
	private final String[] types;
	private final int[] difficulty;
	
	private FileType(final int easy, final int medium, final int hard, final int hardest, String... types)
	{
		this.types = types;
		difficulty = new int[]{easy, medium, hard, hardest};
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

	public int difficulty(long length) {
		//convert to kb
		length /= 1000L;
		
		if (length < difficulty[0])
		{
			return 1;
		}
		else if (length < difficulty[1])
		{
			return 2;
		}
		else if (length < difficulty[2])
		{
			return 3;
		}
		else if (length < difficulty[3])
		{
			return 4;
		}
		return 5;
	}
}
