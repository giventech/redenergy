package simplenem12;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SimpleNem12ParserImpl implements SimpleNem12Parser {

	public static DateTimeFormatter formatter = DateTimeFormatter.BASIC_ISO_DATE;

	@Override
	public Collection<MeterRead> parseSimpleNem12(File simpleNem12File) {
		Collection<MeterRead> meterReadCollection = null;
		try {
			
			//Map all files to MeterRead(s) or MeterVolume(s)�
			List<Object> meterReadAndVolumes =	mapMeterReadAndVolumes(simpleNem12File);
			
			// Extract MeterRead and map it into a Collection<MeterRead>�
			meterReadCollection = meterReadAndVolumes
			.stream().filter(line -> line instanceof MeterRead)
			.map(line -> ((MeterRead)line))
			.collect(Collectors.toCollection(ArrayList<MeterRead>::new));
			
			//Append volumes� in the appropriate MeterRead
			appendVolumesToMeters(meterReadCollection,meterReadAndVolumes);
		} catch (IOException e) {
			e.printStackTrace();
		}
	return meterReadCollection;
	}

	
	/**
	* Trim� RecordType 100 and 900
	* Map all lines to MeterRead or MeterVolumes depending record type 200 or 300�
	* @param simpleNem12File
	* @return
	* @throws IOException
	*/
	public static List<Object> mapMeterReadAndVolumes(File simpleNem12File) throws IOException{
		// Trim� RecordType 100 and 900
			return Files.lines(Paths.get(simpleNem12File.getPath())).filter(lines -> (!lines.startsWith("100") && !lines.startsWith("900")))
			// Map all lines to MeterRead or MeterVolumes depending record type 200 or 300�
			.map(lines -> {
				String linesRead[] = lines.split(",");
				Object obj = null;
				if (linesRead[0].equals("200")) {
					// Always KWH
					obj = new MeterRead(linesRead[1], EnergyUnit.KWH);
				} else if (linesRead[0].equals("300")) {
					LocalDate localDate = LocalDate.parse(linesRead[1], formatter);
					// Either A or� E
					Quality quality = linesRead[3].equals("A") ? Quality.A : Quality.E;
					MeterVolume aMeterVolume = new MeterVolume(new BigDecimal(linesRead[2]), quality);
					obj = (Map<LocalDate, MeterVolume>) Collections.singletonMap(localDate,aMeterVolume);
						
				}
				return obj;
			})
			.collect(Collectors.toList());
	}

	/**
	 * Append volumes from files in the appropriate MeterRead
	 * 
	 * @param meterReadCollections
	 * @param meterReadAndVolumes
	 */
	public static void appendVolumesToMeters(Collection<MeterRead> meterReadCollections,List<Object> meterReadAndVolumes) {
		
		MeterRead tempMeterRead = null;
		for (MeterRead aMeterRead : meterReadCollections) {
			for (Object objectStreamed : meterReadAndVolumes) {
				if (objectStreamed instanceof MeterRead) {
					tempMeterRead = (MeterRead) objectStreamed;
				} else if (objectStreamed instanceof Map<?, ?> && tempMeterRead.equals((MeterRead) aMeterRead)) {
					LocalDate uniqueDate = (LocalDate) ((Map<?, ?>) objectStreamed).keySet().stream().findFirst()
							.orElse(null);
					MeterVolume value = (MeterVolume) ((Map<?, ?>) objectStreamed).values().stream().findFirst()
							.orElse(null);
					tempMeterRead.appendVolume(uniqueDate, value);
				}
			}
		}
	}
}

