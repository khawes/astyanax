package com.netflix.astyanax.cql.test;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import junit.framework.Assert;

import org.joda.time.DateTime;
import org.junit.BeforeClass;
import org.junit.Test;

import com.netflix.astyanax.MutationBatch;
import com.netflix.astyanax.model.Column;
import com.netflix.astyanax.model.ColumnFamily;
import com.netflix.astyanax.model.ColumnList;
import com.netflix.astyanax.serializers.BytesArraySerializer;
import com.netflix.astyanax.serializers.IntegerSerializer;
import com.netflix.astyanax.serializers.StringSerializer;


public class ReadTests extends KeyspaceTests {

	public static DateTime OriginalDate = new DateTime().withMillisOfSecond(0).withSecondOfMinute(0).withMinuteOfHour(0).withHourOfDay(0);
	public static byte[] TestBytes = new String("TestBytes").getBytes();
	public static UUID TestUUID = UUID.fromString("edeb3d70-15ce-11e3-8ffd-0800200c9a66");
	public static int RowCount = 1;
	
	public static String[] columnNamesArr = {"firstname", "lastname", "address","age","ageShort", "ageLong","percentile", "married","single", "birthdate", "bytes", "uuid", "empty"};
	public static List<String> columnNames = new ArrayList<String>(Arrays.asList(columnNamesArr));
	
	public static ColumnFamily<String, String> CF_USER_INFO = ColumnFamily.newColumnFamily(
			"UserInfo", // Column Family Name
			StringSerializer.get(), // Key Serializer
			StringSerializer.get()); // Column Serializer

	public static ColumnFamily<String, String> CF_COLUMN_RANGE_TEST = ColumnFamily.newColumnFamily(
			"columnrange", // Column Family Name
			StringSerializer.get(), // Key Serializer
			StringSerializer.get(), // Column Serializer
			IntegerSerializer.get()); // Data serializer;


	public static void initReadTests() throws Exception {
		initContext();
		Collections.sort(columnNames); 
	}
	


	private void createKeyspace() throws Exception {
		keyspace.createColumnFamily(CF_USER_INFO, null);
	}

    public void testAllColumnsForRow(ColumnList<String> columns, int i) throws Exception {

    	Date date = OriginalDate.plusMinutes(i).toDate();

    	testColumnValue(columns, "firstname", columnNames, "john_" + i);
    	testColumnValue(columns, "lastname", columnNames, "smith_" + i);
    	testColumnValue(columns, "address", columnNames, "john smith address " + i);
    	testColumnValue(columns, "age", columnNames, 30 + i);
    	testColumnValue(columns, "ageShort", columnNames, new Integer(30+i).shortValue());
    	testColumnValue(columns, "ageLong", columnNames, new Integer(30+i).longValue());
    	testColumnValue(columns, "percentile", columnNames, 30.1);
    	testColumnValue(columns, "married", columnNames, true);
    	testColumnValue(columns, "single", columnNames, false);
    	testColumnValue(columns, "birthdate", columnNames, date);
    	testColumnValue(columns, "bytes", columnNames, TestBytes);
    	testColumnValue(columns, "uuid", columnNames, TestUUID);
    	testColumnValue(columns, "empty", columnNames, null);
    	
    	/** TEST THE ITERATOR INTERFACE */
    	Iterator<Column<String>> iter = columns.iterator();
    	Iterator<String> columnNameIter = columnNames.iterator();
    	while (iter.hasNext()) {
    		Column<String> col = iter.next();
    		String columnName = columnNameIter.next();
    		Assert.assertEquals(columnName, col.getName());
    	}
    }
    
    
    private <T> void testColumnValue(ColumnList<String> response, String columnName, List<String> columnNames, T value) {
    	
    	// by column name
    	Column<String> column = response.getColumnByName(columnName);
    	Assert.assertEquals(columnName, column.getName());
    	testColumnValue(column, value);
    	
    	// by column index
    	int index = columnNames.indexOf(columnName);
    	column = response.getColumnByIndex(index);
    	testColumnValue(column, value);
    }
    
    private <T> void testColumnValue(Column<String> column, T value) {

    	// Check the column name
    	// check if value exists
    	if (value != null) {
    		Assert.assertTrue(column.hasValue());
    		if (value instanceof String) {
        		Assert.assertEquals(value, column.getStringValue());
    		} else if (value instanceof Integer) {
        		Assert.assertEquals(value, column.getIntegerValue());
    		} else if (value instanceof Short) {
        		Assert.assertEquals(value, column.getShortValue());
    		} else if (value instanceof Long) {
        		Assert.assertEquals(value, column.getLongValue());
    		} else if (value instanceof Double) {
        		Assert.assertEquals(value, column.getDoubleValue());
    		} else if (value instanceof Boolean) {
        		Assert.assertEquals(value, column.getBooleanValue());
    		} else if (value instanceof Date) {
        		Assert.assertEquals(value, column.getDateValue());
    		} else if (value instanceof byte[]) {
    			ByteBuffer bbuf = column.getByteBufferValue();
    			String result = new String(BytesArraySerializer.get().fromByteBuffer(bbuf));
    			Assert.assertEquals(new String((byte[])value), result);
    		} else if (value instanceof UUID) {
        		Assert.assertEquals(value, column.getUUIDValue());
    		} else {
    			Assert.fail("Value not recognized for column: " + column.getName()); 
    		}
    	} else {
    		// check that value does not exist
    		Assert.assertFalse(column.hasValue());
    	}
    }
	public void populateRows() throws Exception {

        MutationBatch mb = keyspace.prepareMutationBatch();

        for (int i=0; i<RowCount; i++) {
        	
        	Date date = OriginalDate.plusMinutes(i).toDate();
            mb.withRow(CF_USER_INFO, "acct_" + i)
            .putColumn("firstname", "john_" + i, null)
            .putColumn("lastname", "smith_" + i, null)
            .putColumn("address", "john smith address " + i, null)
            .putColumn("age", 30+i, null)
            .putColumn("ageShort", new Integer(30+i).shortValue(), null)
            .putColumn("ageLong", new Integer(30+i).longValue(), null)
            .putColumn("percentile", 30.1)
            .putColumn("married", true)
            .putColumn("single", false)
            .putColumn("birthdate", date)
            .putColumn("bytes", TestBytes)
            .putColumn("uuid", TestUUID)
            .putEmptyColumn("empty");

            mb.execute();
            mb.discardMutations();
        }
	}
	
	
	public void deleteRows() throws Exception {

        MutationBatch mb = keyspace.prepareMutationBatch();

        for (int i=0; i<RowCount; i++) {
            mb.withRow(CF_USER_INFO, "acct_" + i).delete();
            mb.execute();
            mb.discardMutations();
        }
	}
	
	public List<TestTokenRange> getTestTokenRanges() {
		
		/**
		 * HERE IS THE ACTUAL ORDER OF KEYS SORTED BY THEIR TOKENS	
		 * 
		 * -1671667184962092389   = Q,   -1884162317724288694   = O,   -2875471597373478633   = K, 
		 * -300136452241384611    = L,   -4197513287269367591   = H,   -422884050476930919    = Y, 
		 * -4837624800923759386   = B,   -4942250469937744623   = W,   -7139673851965614954   = J, 
		 * -7311855499978618814   = X,   -7912594386904524724   = M,   -8357705097978550118   = T, 
		 * -8692134701444027338   = C,    243126998722523514    = A,    3625209262381227179   = F, 
		 *  3846318681772828433   = R,    3914548583414697851   = N,    4834152074310082538   = I, 
		 *  4943864740760620945   = S,    576608558731393772    = V,    585625305377507626    = G, 
		 *  7170693507665539118   = E,    8086064298967168788   = Z,    83360928582194826     = P, 
		 *  8889191829175541774   = D,    9176724567785656400   = U
		 * 
		 */
		List<TestTokenRange> tokenRanges = new ArrayList<TestTokenRange>();
		
		
		tokenRanges.add(new TestTokenRange("-8692134701444027338", "-7912594386904524724","C", "T", "M"));
		tokenRanges.add(new TestTokenRange("-7311855499978618814", "-4942250469937744623","X", "J", "W"));
		tokenRanges.add(new TestTokenRange("-4837624800923759386", "-2875471597373478633","B", "H", "K"));
		tokenRanges.add(new TestTokenRange("-1884162317724288694", "-422884050476930919","O", "Q", "Y"));
		tokenRanges.add(new TestTokenRange("-300136452241384611", "243126998722523514","L", "P", "A"));
		tokenRanges.add(new TestTokenRange("576608558731393772", "3625209262381227179","V", "G", "F"));
		tokenRanges.add(new TestTokenRange("3846318681772828433", "4834152074310082538","R", "N", "I"));
		tokenRanges.add(new TestTokenRange("4943864740760620945", "8086064298967168788","S", "E", "Z"));
		tokenRanges.add(new TestTokenRange("8889191829175541774", "9176724567785656400","D", "U"));

		return tokenRanges;
		
//		SortedMap<String, String> reverseMapping2 = new TreeMap<String, String>();
//		for (LinkedHashMap<String, String> map2 : tokenRanges) {
//			reverseMapping2.putAll(map2);	
//		}
//		
//		SortedMap<String, String> md5Mapping = new TreeMap<String, String>();
//		SortedMap<String, String> reverseMapping = new TreeMap<String, String>();
//		
//		
//		for (char ch = 'A'; ch <='Z'; ch++) {
//			
//			String s = String.valueOf(ch);
//			ByteBuffer bb = StringSerializer.get().toByteBuffer(s);
//			String hash = Murmur3Partitioner.get().getTokenForKey(bb);
//			
//			md5Mapping.put(s, hash);
//			reverseMapping.put(hash, s);
//		}
//		
//		System.out.println("Map " + md5Mapping);
//		System.out.println("Reverse Map " + reverseMapping);
//		
//		for (String key : reverseMapping.keySet()) {
//			String v1 = reverseMapping.get(key);
//			String v2 = reverseMapping2.get(key);
//			if (!v1.equals(v2)) {
//				System.out.println("Mismatch: " + key + " " + v1 + " " + v2);
//			}
//		}
	}
	
	public static class TestTokenRange {
		
		public String startToken;
		public String endToken;
		List<String> expectedRowKeys = new ArrayList<String>();
		
		public TestTokenRange(String start, String end, String ... expectedKeys) {
			startToken = start;
			endToken = end;
			expectedRowKeys.addAll(Arrays.asList(expectedKeys));
		}
	}
	

	public Collection<String> getRandomColumns(int numColumns) {

		Random random = new Random();
		Set<String> hashSet = new HashSet<String>();

		while(hashSet.size() < numColumns) {
			int pick = random.nextInt(26);
			char ch = (char) ('a' + pick);
			hashSet.add(String.valueOf(ch));
		}
		return hashSet;
	}
    
}
