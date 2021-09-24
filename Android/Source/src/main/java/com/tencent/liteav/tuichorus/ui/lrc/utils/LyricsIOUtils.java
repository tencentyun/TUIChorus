package com.tencent.liteav.tuichorus.ui.lrc.utils;

import com.tencent.liteav.tuichorus.ui.lrc.formats.LyricsFileReader;
import com.tencent.liteav.tuichorus.ui.lrc.formats.vtt.VttLyricsFileReader;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class LyricsIOUtils {
	private static ArrayList<LyricsFileReader> readers;

	static {
		readers = new ArrayList<>();
		readers.add(new VttLyricsFileReader());
	}

	/**
	 * 获取支持的歌词文件格式
	 * 
	 * @return
	 */
	public static List<String> getSupportLyricsExts() {
		List<String> lrcExts = new ArrayList<String>();
		for (LyricsFileReader lyricsFileReader : readers) {
			lrcExts.add(lyricsFileReader.getSupportFileExt());
		}
		return lrcExts;
	}

	/**
	 * 获取歌词文件读取器
	 * 
	 * @param file
	 * @return
	 */
	public static LyricsFileReader getLyricsFileReader(File file) {
		return getLyricsFileReader(file.getName());
	}

	/**
	 * 获取歌词文件读取器
	 * 
	 * @param fileName
	 * @return
	 */
	public static LyricsFileReader getLyricsFileReader(String fileName) {
		String ext = Utils.getFileExt(fileName);
		for (LyricsFileReader lyricsFileReader : readers) {
			if (lyricsFileReader.isFileSupported(ext)) {
				return lyricsFileReader;
			}
		}
		return null;
	}
}
