package core.util;

import java.util.Comparator;

import com.badlogic.gdx.files.FileHandle;

public class FileSort implements Comparator<FileHandle> {

	@Override
	public int compare(FileHandle o1, FileHandle o2) {
		
		if (o1 == null && o2 == null)
			return 0;
		if (o1 == null)
			return 1;
		if (o2 == null)
			return -1;
		
		if (o1.isDirectory() && !o2.isDirectory())
		{
			return -1;
		}
		if (!o1.isDirectory() && o2.isDirectory())
		{
			return 1;
		}
		return o1.name().toLowerCase().compareTo(o2.name().toLowerCase());
	}

}
