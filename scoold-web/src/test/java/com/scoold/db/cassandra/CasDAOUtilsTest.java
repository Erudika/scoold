/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.scoold.db.cassandra;


/**
 *
 * @author alexb
 */
public class CasDAOUtilsTest {

//	private static EmbeddedServerHelper embedded;
//	private static final Logger logger = Logger.getLogger(CasDAOUtilsTest.class.getName());
//	private static User testObject;
//	private static CasDAOUtils cdu = new CasDAOUtils();

    public CasDAOUtilsTest() {
    }

//	@BeforeClass
//	public static void setUpClass() throws Exception {
////		embedded = new EmbeddedServerHelper();
////		embedded.setup();
////
////		testObject = new User("user@test.com", true, User.UserType.ALUMNUS, "Test User");
////		testObject.setAboutme("Test about me");
////		testObject.setDob(12314325234L);
////		testObject.setLocation("Test location");
//	}

//	@AfterClass
//	public static void tearDownClass() throws Exception {
////		embedded.teardown();
//	}
//
//	/**
//	 * Test of getKeyspace method, of class CasDAOUtils.
//	 */ @Test
//	public void testGetKeyspace() {
//		assertNotNull(cdu.getKeyspace());
//	}
//
//	
//	/**
//	 * Test of create method, of class CasDAOUtils.
//	 */ @Test
//	public void testCreate_PObject_CasDAOFactoryCF() {
//		Long result = cdu.create(testObject, USERS);
//		assertTrue(result > 0L);
//
//		User userOut = cdu.read(User.class, result.toString(), USERS);
//		assertEquals(testObject, userOut);
//
//		assertNull(cdu.create(null, USERS));
//		assertNull(cdu.create(testObject, null));
//	}
//
//	/**
//	 * Test of create method, of class CasDAOUtils.
//	 */ @Test
//	public void testCreate_3args() {
//		String TEST_KEY = "testkey";
//		Mutator<String> mut = cdu.createMutator();
//		Long result = cdu.create(TEST_KEY, testObject, USERS, mut);
//		mut.execute();
//		assertTrue(result > 0L);
//
//		User userOut = cdu.read(User.class, TEST_KEY, USERS);
//		assertEquals(testObject, userOut);
//	}
//
//	/**
//	 * Test of read method, of class CasDAOUtils.
//	 */ @Test
//	public void testRead() {
//		assertNotNull(cdu.read(User.class, testObject.getId().toString(), USERS));
//		assertNull(cdu.read(null, "", USERS));
//		assertNull(cdu.read(null, "key", null));
//		assertNull(cdu.read(User.class, "key", null));
//	}
//
//	/**
//	 * Test of update method, of class CasDAOUtils.
//	 */ @Test
//	public void testUpdate_PObject_CasDAOFactoryCF() {
//		String newName = RandomStringUtils.randomAlphabetic(10);
//		testObject.setFullname(newName);
//		cdu.update(testObject, USERS);
//		User out = cdu.read(User.class, testObject.getId().toString(), USERS);
//		assertEquals(newName, out.getName());
//	}
//
//	/**
//	 * Test of update method, of class CasDAOUtils.
//	 */ @Test
//	public void testUpdate_3args() {
//		String key = "testkey";
//		String newName = RandomStringUtils.randomAlphabetic(10);
//		testObject.setFullname(newName);
//		Mutator<String> mut = cdu.createMutator();
//		cdu.update(key, testObject, USERS, mut);
//		mut.execute();
//		User out = cdu.read(User.class, key, USERS);
//		assertEquals(newName, out.getName());
//	}
//
//	/**
//	 * Test of delete method, of class CasDAOUtils.
//	 */ @Test
//	public void testDelete_PObject_CasDAOFactoryCF() {
//		cdu.delete(testObject, USERS);
//		assertNull(cdu.read(User.class, testObject.getId().toString(), USERS));
//	}
//
//	/**
//	 * Test of delete method, of class CasDAOUtils.
//	 */ @Test
//	public void testDelete_3args() {
//		String key = "testkey";
//
//		Mutator<String> mut = cdu.createMutator();
//		cdu.delete(key, testObject, USERS, mut);
//		mut.execute();
//		assertNull(cdu.read(User.class, key, USERS));
//	}
//
//	/**
//	 * Test of deleteAll method, of class CasDAOUtils.
//	 */ @Test
//	public void testDeleteAll() {
//		User u = new User("batch@batch.com", true, User.UserType.STUDENT, "Batch test");
//		cdu.create(u, USERS);
//		cdu.putColumn(u.getId(), USERS_IDS, u.getId().toString(), u.getId());
//		cdu.putColumn(u.getId(), MEDIA_PARENTIDS, 1234567L, 1234567L);
//		cdu.putColumn(u.getEmail(), EMAILS, u.getId().toString(), u.getId());
//
//		Mutator<String> mut = cdu.createMutator();
//		
//		CasDAOUtils.addDeletion(new Column(u.getId().toString(), USERS), mut);
//		CasDAOUtils.addDeletion(new Column(u.getId(), USERS_IDS), mut);
//		CasDAOUtils.addDeletion(new Column(u.getId(), MEDIA_PARENTIDS), mut);
//		CasDAOUtils.addDeletion(new Column(u.getEmail(), EMAILS), mut);
//
//		mut.execute();
//		
//		assertNull(cdu.read(User.class, u.getId().toString(), USERS));
//		assertNull(cdu.getColumn(u.getId(), USERS_IDS, u.getId().toString()));
//		assertNull(cdu.getColumn(u.getId(), MEDIA_PARENTIDS, 1234567L));
//		assertNull(cdu.getColumn(u.getEmail(), EMAILS, u.getId().toString()));
//	}
//
//	/**
//	 * Test of putColumn method, of class CasDAOUtils.
//	 */ @Test
//	public void testPutColumn_4args() {
//		String key = "test@email.com";
//		cdu.putColumn(key, EMAILS, "test", "test");
//		assertEquals("test", cdu.getColumn(key, EMAILS, "test"));
//
//		cdu.removeColumn(key, EMAILS, "test");
//		assertNull(cdu.getColumn(key, EMAILS, "test"));
//	}
//
//	/**
//	 * Test of putColumn method, of class CasDAOUtils.
//	 */ @Test
//	public void testPutColumn_5args() throws Exception{
//		String key = "test@email.com";
//		cdu.putColumn(key, EMAILS, "test", "test", 2);
//		assertEquals("test", cdu.getColumn(key, EMAILS, "test"));
//
//		Thread.sleep(3000);
//		assertNull(cdu.getColumn(key, EMAILS, "test"));
//	}
//
//	/**
//	 * Test of putSuperColumn method, of class CasDAOUtils.
//	 */ @Test
//	public void testPutSuperColumn() {
//		System.out.println("putSuperColumn");
//		String key = "";
//		SCF<SN, SUBN> cf = null;
//		Object colName = null;
//		List<HColumn<SUBN>> colValue = null;
//		putSuperColumn(key, cf, colName, colValue);
//		fail("The test case is a prototype.");
//	}
//
//	/**
//	 * Test of getColumn method, of class CasDAOUtils.
//	 */ @Test
//	public void testGetColumn() {
//		System.out.println("getColumn");
//		String key = "";
//		CF<N> cf = null;
//		Object colName = null;
//		Object expResult = null;
//		Object result = getColumn(key, cf, colName);
//		assertEquals(expResult, result);
//		fail("The test case is a prototype.");
//	}
//
//	/**
//	 * Test of getSuperColumn method, of class CasDAOUtils.
//	 */ @Test
//	public void testGetSuperColumn() {
//		System.out.println("getSuperColumn");
//		String key = "";
//		SCF<SN, SUBN> cf = null;
//		Object colName = null;
//		Class<SUBN> subColClass = null;
//		List expResult = null;
//		List result = getSuperColumn(key, cf, colName, subColClass);
//		assertEquals(expResult, result);
//		fail("The test case is a prototype.");
//	}
//
//	/**
//	 * Test of getSubColumn method, of class CasDAOUtils.
//	 */ @Test
//	public void testGetSubColumn() {
//		System.out.println("getSubColumn");
//		String key = "";
//		SCF<SN, SUBN> cf = null;
//		Object colName = null;
//		Object subColName = null;
//		Object expResult = null;
//		Object result = getSubColumn(key, cf, colName, subColName);
//		assertEquals(expResult, result);
//		fail("The test case is a prototype.");
//	}
//
//	/**
//	 * Test of removeColumn method, of class CasDAOUtils.
//	 */ @Test
//	public void testRemoveColumn() {
//		System.out.println("removeColumn");
//		String key = "";
//		CF<N> cf = null;
//		Object colName = null;
//		removeColumn(key, cf, colName);
//		fail("The test case is a prototype.");
//	}
//
//	/**
//	 * Test of removeSuperColumn method, of class CasDAOUtils.
//	 */ @Test
//	public void testRemoveSuperColumn() {
//		System.out.println("removeSuperColumn");
//		String key = "";
//		SCF<SN, ?> cf = null;
//		Object colName = null;
//		removeSuperColumn(key, cf, colName);
//		fail("The test case is a prototype.");
//	}
//
//	/**
//	 * Test of getHColumn method, of class CasDAOUtils.
//	 */ @Test
//	public void testGetHColumn() {
//		System.out.println("getHColumn");
//		String key = "";
//		CF<N> cf = null;
//		Object colName = null;
//		HColumn expResult = null;
//		HColumn result = getHColumn(key, cf, colName);
//		assertEquals(expResult, result);
//		fail("The test case is a prototype.");
//	}
//
//	/**
//	 * Test of getHSuperColumn method, of class CasDAOUtils.
//	 */ @Test
//	public void testGetHSuperColumn() {
//		System.out.println("getHSuperColumn");
//		String key = "";
//		SCF<SN, SUBN> cf = null;
//		Object colName = null;
//		Class<SUBN> subColClass = null;
//		HSuperColumn expResult = null;
//		HSuperColumn result = getHSuperColumn(key, cf, colName, subColClass);
//		assertEquals(expResult, result);
//		fail("The test case is a prototype.");
//	}
//
//	/**
//	 * Test of getHSubColumn method, of class CasDAOUtils.
//	 */ @Test
//	public void testGetHSubColumn() {
//		System.out.println("getHSubColumn");
//		String key = "";
//		SCF<SN, SUBN> cf = null;
//		Object colName = null;
//		Object subColName = null;
//		HColumn expResult = null;
//		HColumn result = getHSubColumn(key, cf, colName, subColName);
//		assertEquals(expResult, result);
//		fail("The test case is a prototype.");
//	}
//
//	/**
//	 * Test of createRow method, of class CasDAOUtils.
//	 */ @Test
//	public void testCreateRow() {
//		System.out.println("createRow");
//		String key = "";
//		CF<N> cf = null;
//		List<HColumn<N>> row = null;
//		String expResult = "";
//		String result = createRow(key, cf, row);
//		assertEquals(expResult, result);
//		fail("The test case is a prototype.");
//	}
//
//	/**
//	 * Test of createSuperRow method, of class CasDAOUtils.
//	 */ @Test
//	public void testCreateSuperRow() {
//		System.out.println("createSuperRow");
//		String key = "";
//		SCF<SN, SUBN> cf = null;
//		List<HSuperColumn<SN, SUBN>> row = null;
//		String expResult = "";
//		String result = createSuperRow(key, cf, row);
//		assertEquals(expResult, result);
//		fail("The test case is a prototype.");
//	}
//
//	/**
//	 * Test of readRow method, of class CasDAOUtils.
//	 */ @Test
//	public void testReadRow() {
//		System.out.println("readRow");
//		String key = "";
//		CF<N> cf = null;
//		Class<N> colNameClass = null;
//		Object startKey = null;
//		MutableLong page = null;
//		MutableLong itemcount = null;
//		int maxItems = 0;
//		boolean reverse = false;
//		List expResult = null;
//		List result = readRow(key, cf, colNameClass, startKey, page, itemcount, maxItems, reverse);
//		assertEquals(expResult, result);
//		fail("The test case is a prototype.");
//	}
//
//	/**
//	 * Test of readSuperRow method, of class CasDAOUtils.
//	 */ @Test
//	public void testReadSuperRow() {
//		System.out.println("readSuperRow");
//		String key = "";
//		SCF<SN, SUBN> cf = null;
//		Class<SN> superColNameClass = null;
//		Class<SUBN> colNameClass = null;
//		Object startKey = null;
//		MutableLong page = null;
//		MutableLong itemcount = null;
//		int maxItems = 0;
//		boolean reverse = false;
//		List expResult = null;
//		List result = readSuperRow(key, cf, superColNameClass, colNameClass, startKey, page, itemcount, maxItems, reverse);
//		assertEquals(expResult, result);
//		fail("The test case is a prototype.");
//	}
//
//	/**
//	 * Test of deleteRow method, of class CasDAOUtils.
//	 */ @Test
//	public void testDeleteRow() {
//		System.out.println("deleteRow");
//		String key = "";
//		CF<?> cf = null;
//		deleteRow(key, cf);
//		fail("The test case is a prototype.");
//	}
//
//	/**
//	 * Test of deleteSuperRow method, of class CasDAOUtils.
//	 */ @Test
//	public void testDeleteSuperRow() {
//		System.out.println("deleteSuperRow");
//		String key = "";
//		SCF<?, ?> cf = null;
//		deleteSuperRow(key, cf);
//		fail("The test case is a prototype.");
//	}
//
//	/**
//	 * Test of readAll method, of class CasDAOUtils.
//	 */ @Test
//	public void testReadAll_12args() {
//		System.out.println("readAll");
//		Class<T> clazz = null;
//		String keysKey = "";
//		CF<N> keysCf = null;
//		CF<String> valsCf = null;
//		Class<N> colNameClass = null;
//		Object startKey = null;
//		MutableLong page = null;
//		MutableLong itemcount = null;
//		int maxItems = 0;
//		boolean reverse = false;
//		boolean colNamesAreKeys = false;
//		boolean countOnlyColumns = false;
//		ArrayList expResult = null;
//		ArrayList result = readAll(clazz, keysKey, keysCf, valsCf, colNameClass, startKey, page, itemcount, maxItems, reverse, colNamesAreKeys, countOnlyColumns);
//		assertEquals(expResult, result);
//		fail("The test case is a prototype.");
//	}
//
//	/**
//	 * Test of readAll method, of class CasDAOUtils.
//	 */ @Test
//	public void testReadAll_3args() {
//		System.out.println("readAll");
//		Class<T> clazz = null;
//		List<String> keys = null;
//		CF<String> cf = null;
//		ArrayList expResult = null;
//		ArrayList result = readAll(clazz, keys, cf);
//		assertEquals(expResult, result);
//		fail("The test case is a prototype.");
//	}
//
//	/**
//	 * Test of existsColumn method, of class CasDAOUtils.
//	 */ @Test
//	public void testExistsColumn() {
//		System.out.println("existsColumn");
//		String key = "";
//		CF<N> cf = null;
//		Object columnName = null;
//		boolean expResult = false;
//		boolean result = existsColumn(key, cf, columnName);
//		assertEquals(expResult, result);
//		fail("The test case is a prototype.");
//	}
//
//	/**
//	 * Test of existsSuperColumn method, of class CasDAOUtils.
//	 */ @Test
//	public void testExistsSuperColumn() {
//		System.out.println("existsSuperColumn");
//		String key = "";
//		SCF<SN, SUBN> cf = null;
//		Object columnName = null;
//		Class<SUBN> subColClass = null;
//		boolean expResult = false;
//		boolean result = existsSuperColumn(key, cf, columnName, subColClass);
//		assertEquals(expResult, result);
//		fail("The test case is a prototype.");
//	}
//
//	/**
//	 * Test of existsSubColumn method, of class CasDAOUtils.
//	 */ @Test
//	public void testExistsSubColumn() {
//		System.out.println("existsSubColumn");
//		String key = "";
//		SCF<SN, SUBN> cf = null;
//		Object columnName = null;
//		Object subColName = null;
//		boolean expResult = false;
//		boolean result = existsSubColumn(key, cf, columnName, subColName);
//		assertEquals(expResult, result);
//		fail("The test case is a prototype.");
//	}
//
//	/**
//	 * Test of countColumns method, of class CasDAOUtils.
//	 */ @Test
//	public void testCountColumns() {
//		System.out.println("countColumns");
//		String key = "";
//		CF<N> cf = null;
//		Class<N> colNameClass = null;
//		int expResult = 0;
//		int result = countColumns(key, cf, colNameClass);
//		assertEquals(expResult, result);
//		fail("The test case is a prototype.");
//	}
//
//	/**
//	 * Test of countSuperColumns method, of class CasDAOUtils.
//	 */ @Test
//	public void testCountSuperColumns() {
//		System.out.println("countSuperColumns");
//		String key = "";
//		SCF<SN, ?> cf = null;
//		Class<SN> clazz = null;
//		int expResult = 0;
//		int result = countSuperColumns(key, cf, clazz);
//		assertEquals(expResult, result);
//		fail("The test case is a prototype.");
//	}
//
//	/**
//	 * Test of countSubColumns method, of class CasDAOUtils.
//	 */ @Test
//	public void testCountSubColumns() {
//		System.out.println("countSubColumns");
//		String key = "";
//		SCF<SN, SUBN> cf = null;
//		Object superCol = null;
//		Class<SUBN> subColClass = null;
//		int expResult = 0;
//		int result = countSubColumns(key, cf, superCol, subColClass);
//		assertEquals(expResult, result);
//		fail("The test case is a prototype.");
//	}
//
//	/**
//	 * Test of getCount method, of class CasDAOUtils.
//	 */ @Test
//	public void testGetCount() {
//		System.out.println("getCount");
//		Class<T> clazz = null;
//		Long expResult = null;
//		Long result = getCount(clazz);
//		assertEquals(expResult, result);
//		fail("The test case is a prototype.");
//	}
//
//	/**
//	 * Test of getBeanCount method, of class CasDAOUtils.
//	 */ @Test
//	public void testGetBeanCount() {
//		System.out.println("getBeanCount");
//		Class<T> clazz = null;
//		CasDAOUtils instance = new CasDAOUtils();
//		Long expResult = null;
//		Long result = instance.getBeanCount(clazz);
//		assertEquals(expResult, result);
//		fail("The test case is a prototype.");
//	}
//
//	/**
//	 * Test of updateBeanCount method, of class CasDAOUtils.
//	 */ @Test
//	public void testUpdateBeanCount() {
//		System.out.println("updateBeanCount");
//		Class<?> clazz = null;
//		boolean decrement = false;
//		updateBeanCount(clazz, decrement);
//		fail("The test case is a prototype.");
//	}

//	/**
//	 * Test of voteUp method, of class CasDAOUtils.
//	 */ @Test
//	public void testVote() throws Exception{
//
//		Post p = new Post();
//		p.setTitle("test");
//		p.setBody("test");
//		p.setTags(",test,");
//		p.setUserid(123L);
//		p.setId(cdu.getID());
//		
//		String id = 1234L;
//		long v = 2000L;
//		long vla = cdu.convertMsTimeToCasTime(cdu.getKeyspace(), v); // ms
//		int vlfs = 5; // s
//
//		CasDAOUtils cdu = new CasDAOUtils();
//
//		assertTrue(p.getVotes() == 0);
//		assertFalse(cdu.vote(p.getCreatorid(), p, true, vla, vlfs));
//
//		assertTrue(cdu.vote(id, p, true, vla, vlfs));	//1+
//		assertTrue(p.getVotes() == 1);
//
//		assertTrue(cdu.vote(id, p, false, vla, vlfs));	//2-
//		assertTrue(p.getVotes() == 0);
//
//		assertTrue(cdu.vote(id, p, true, vla, vlfs));	//3+
//		assertTrue(p.getVotes() == 1);
//
//		assertTrue(cdu.vote(id, p, false, vla, vlfs));	//4-
//		assertTrue(p.getVotes() == 0);
//
//		assertTrue(cdu.vote(id, p, true, vla, vlfs));	//5+
//		assertTrue(p.getVotes() == 1);
//
//		assertFalse(cdu.vote(id, p, true, vla, vlfs));
//		assertTrue(p.getVotes() == 1);
//
//		assertTrue(cdu.vote(id, p, false, vla, vlfs));	//6-
//		assertTrue(cdu.vote(id, p, false, vla, vlfs));	//7-
//		assertTrue(p.getVotes() == -1);
//
//		assertFalse(cdu.vote(id, p, false, vla, vlfs));
//		assertTrue(p.getVotes() == -1);
//
//		assertTrue(cdu.vote(id, p, true, vla, vlfs));	//8+
//		assertTrue(cdu.vote(id, p, true, vla, vlfs));	//9+
//		assertTrue(p.getVotes() == 1);
//
//		Thread.sleep(v);
//
//		assertFalse(cdu.vote(id, p, true, vla, vlfs));
//		assertTrue(p.getVotes() == 1);
//
//		Thread.sleep(vlfs * 1000);
//
//		assertTrue(cdu.vote(id, p, true, vla, vlfs));
//		assertTrue(p.getVotes() == 2);
//	}
//
//	/**
//	 * Test of getNewId method, of class CasDAOUtils.
//	 */ @Test
//	public void testGetNewId() {
//		String id1 = cdu.getNewId();
//		String id2 = cdu.getNewId();
//		String id3 = cdu.getNewId();
//		assertFalse(id1 == id2 || id1 == id3 || id2 == id3);
//	}
//
//	/**
//	 * Test of getTimeID method, of class CasDAOUtils.
//	 */ @Test
//	public void testGetTimeID() {
//		ID u1 = cdu.getTimeID();
//		ID u2 = cdu.getTimeID();
//		assertFalse(u1.equals(u2));
//	}
//
//	/**
//	 * Test of getID method, of class CasDAOUtils.
//	 */ @Test
//	public void testGetID() {
//		String u1 = cdu.getID();
//		String u2 = cdu.getID();
//		assertFalse(u1.equals(u2));
//	}
//
//	/**
//	 * Test of addTimesortColumn method, of class CasDAOUtils.
//	 */ @Test
//	public void testAddTimesortColumn() {
//		long now = System.currentTimeMillis();
//		long k1 = 1L;
//		long k2 = 2L;
//		long k3 = 3L;
//		cdu.addTimesortColumn(null, k1,
//			CasDAOFactory.USERS_BY_TIMESTAMP, k1, null);
//
//		assertNotNull(cdu.getColumn(DEFAULT_KEY, USERS_BY_TIMESTAMP, k1));
//
//		cdu.addTimesortColumn(null, k2,
//			CasDAOFactory.USERS_BY_TIMESTAMP, k2, null);
//
//		assertNotNull(cdu.getColumn(DEFAULT_KEY, USERS_BY_TIMESTAMP, k2));
//
//		cdu.addTimesortColumn(null, k3,
//			CasDAOFactory.USERS_BY_TIMESTAMP, now, null);
//
//		assertNotNull(cdu.getColumn(DEFAULT_KEY, USERS_BY_TIMESTAMP, now));
//
//		HColumn<String, String> last = cdu.getLastColumn(DEFAULT_KEY,
//				USERS_BY_TIMESTAMP, String.class, false);
//
//		HColumn<String, String> first = cdu.getLastColumn(DEFAULT_KEY,
//				USERS_BY_TIMESTAMP, String.class, true);
//
//		assertEquals(Long.toString(k1), last.getValue());
//		assertEquals(Long.toString(k3), first.getValue());
//
//		cdu.removeTimesortColumn(DEFAULT_KEY, USERS_BY_TIMESTAMP, now);
//		assertNull(cdu.getColumn(DEFAULT_KEY, USERS_BY_TIMESTAMP, now));
//	}
//
//	/**
//	 * Test of addNumbersortColumn method, of class CasDAOUtils.
//	 */ @Test
//	public void testAddNumbersortColumn() {
//		String compositeKey1 = "100".concat(Utils.SEPARATOR).concat("1231");
//		String compositeKey2 = "200".concat(Utils.SEPARATOR).concat("1232");
//		String compositeKey3 = "300".concat(Utils.SEPARATOR).concat("1233");
//		String compositeKey4 = "400".concat(Utils.SEPARATOR).concat("1234");
//		int max = 3;
//		Mutator<String> mut = cdu.createMutator();
//
//		cdu.addNumbersortColumn(null, USERS_BY_REPUTATION, 1231L, 100, 100, max, mut);
//		assertNull(cdu.getColumn(DEFAULT_KEY, USERS_BY_REPUTATION, compositeKey1));
//
//		cdu.addNumbersortColumn(null, USERS_BY_REPUTATION, 1231L, 100, null, max, mut);
//		assertNotNull(cdu.getColumn(DEFAULT_KEY, USERS_BY_REPUTATION, compositeKey1));
//
//		cdu.addNumbersortColumn(null, USERS_BY_REPUTATION,1232L, 200, null, max, mut);
//		assertNotNull(cdu.getColumn(DEFAULT_KEY, USERS_BY_REPUTATION, compositeKey2));
//
//		cdu.addNumbersortColumn(null, USERS_BY_REPUTATION, 1233L, 300, null, max, mut);
//		assertNotNull(cdu.getColumn(DEFAULT_KEY, USERS_BY_REPUTATION, compositeKey3));
//
//		cdu.addNumbersortColumn(null, USERS_BY_REPUTATION, 1234L, 400, 10, max, mut);
//		assertNotNull(cdu.getColumn(DEFAULT_KEY, USERS_BY_REPUTATION, compositeKey4));
//
//		assertNull(cdu.getColumn(DEFAULT_KEY, USERS_BY_REPUTATION, compositeKey1));
//
//		cdu.removeNumbersortColumn(DEFAULT_KEY, USERS_BY_REPUTATION, 1234L, 400);
//		assertNull(cdu.getColumn(DEFAULT_KEY, USERS_BY_REPUTATION, compositeKey4));
//
//		mut.execute();
//		int count = cdu.countColumns(DEFAULT_KEY, USERS_BY_REPUTATION, String.class);
//		assertTrue(count == 2);
//	}

//	/**
//	 * Test of getLastSuperColumn method, of class CasDAOUtils.
//	 */ @Test
//	public void testGetLastSuperColumn() {
//		String key = "testkey";
//		SCF<String, String> cf = new SCF<String, String>("Super1");
//		Class<String> subColClass = String.class;
//		Serializer<String> strser = cdu.getSerializer(String.class);
//		Serializer<String> lser = cdu.getSerializer(String.class);
//		List<HColumn<String, String>> list = new ArrayList<HColumn<String, String>>();
//		list.add(HFactory.createColumn("test1", "test1", strser, strser));
//		list.add(HFactory.createColumn("test2", "test2", strser, strser));
//
//		cdu.putSuperColumn(key, cf, 1L, list);
//		cdu.putSuperColumn(key, cf, 2L, list);
//		cdu.putSuperColumn(key, cf, 3L, list);
//
//		assertNotNull(cdu.getSuperColumn(key, cf, 1L, subColClass));
//		assertNotNull(cdu.getSuperColumn(key, cf, 2L, subColClass));
//		assertNotNull(cdu.getSuperColumn(key, cf, 3L, subColClass));
//
//		HSuperColumn<?,?,?> last = HFactory.createSuperColumn(1L, list, lser, strser, strser);
//		assertEquals(last.getName(), cdu.getLastSuperColumn(key, cf, String.class, subColClass, false).getName());
//
//		cdu.removeSuperColumn(key, cf, 1L);
//		assertNull(cdu.getSuperColumn(key, cf, 1L, subColClass));
//		assertTrue(cdu.countSuperColumns(key, cf, String.class) == 2);
//
//		cdu.removeSuperColumn(key, cf, 2L);
//		assertNull(cdu.getSuperColumn(key, cf, 2L, subColClass));
//		assertTrue(cdu.countSuperColumns(key, cf, String.class) == 1);
//
//		cdu.removeSuperColumn(key, cf, 3L);
//		assertNull(cdu.getSuperColumn(key, cf, 3L, subColClass));
//		assertTrue(cdu.countSuperColumns(key, cf, String.class) == 0);
//	}


//	/**
//	 * Test of toLong method, of class CasDAOUtils.
//	 */ @Test
//	public void testToLong() {
//		assertNull(CasDAOUtils.toLong(new MutableLong(0)));
//		assertNull(CasDAOUtils.toLong(new MutableLong(1)));
//		assertNull(CasDAOUtils.toLong(null));
//		assertEquals(CasDAOUtils.toLong(new MutableLong(2)), new Long(2));
//	}
//
//	@Test
//	public void testFromColumns() {
//		cdu.create(testObject, USERS);
//		User u = CasDAOUtils.fromColumns(User.class, cdu.readRow(testObject.getId().toString(),
//				USERS, String.class, null, null, null, DEFAULT_LIMIT, false));
//
//		assertEquals(testObject.getId(), u.getId());
//		assertEquals(testObject.getId(), u.getId());
//		assertEquals(testObject.getName(), u.getName());
//		assertEquals(testObject.getEmail(), u.getEmail());
//	}

}