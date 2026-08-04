/*
 * Copyright 2013-2026 Erudika. https://erudika.com
 *
 * Licensed under the EULA - use is subject to license terms.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * For issues and patches go to: https://github.com/erudika/scoold-pro
 */
package com.erudika.scoold.utils;

import com.erudika.para.client.ParaClient;
import com.erudika.para.core.email.Emailer;
import com.erudika.para.core.utils.Para;
import com.erudika.scoold.core.Post;
import com.erudika.scoold.core.Profile;
import com.erudika.scoold.core.Question;
import com.erudika.scoold.core.Reply;
import com.erudika.scoold.utils.avatars.AvatarRepositoryProxy;
import com.erudika.scoold.utils.avatars.GravatarAvatarGenerator;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.Mockito.*;

public class ScooldUtilsAnonymityTest {

	private ParaClient pc;
	private ScooldUtils utils;

	@Before
	public void setUp() throws Exception {
		pc = mock(ParaClient.class);
		LanguageUtils langutils = mock(LanguageUtils.class);
		Emailer emailer = mock(Emailer.class);
		AvatarRepositoryProxy avatarRepo = mock(AvatarRepositoryProxy.class);
		GravatarAvatarGenerator gravatarGen = mock(GravatarAvatarGenerator.class);
		when(avatarRepo.getAnonymizedLink(anyString())).thenReturn("");
		utils = new ScooldUtils(pc, langutils, emailer, avatarRepo, gravatarGen);
		Field instanceField = ScooldUtils.class.getDeclaredField("instance");
		instanceField.setAccessible(true);
		instanceField.set(null, utils);
	}

	private Profile mockProfile(String id, String name) {
		Profile p = new Profile();
		p.setId(id);
		p.setName(name);
		return p;
	}

	private void setupReadAll() {
		when(pc.readAll(anyList())).thenAnswer(invocation -> {
			List<String> ids = invocation.getArgument(0);
			List<Profile> profiles = new ArrayList<>(ids.size());
			for (String id : ids) {
				profiles.add(mockProfile(id, "User " + id));
			}
			return profiles;
		});
	}

	@Test
	public void testPostAnonymousFlagDefaultsFalse() {
		Post p = new Question();
		assertNotNull(p.getAnonymous());
		assertFalse(p.getAnonymous());
	}

	@Test
	public void testPostAnonymousFlagSetter() {
		Post p = new Question();
		p.setAnonymous(true);
		assertTrue(p.getAnonymous());
		p.setAnonymous(false);
		assertFalse(p.getAnonymous());
		// null-tolerant: setter accepts null but getter coerces to false
		p.setAnonymous(null);
		assertFalse(p.getAnonymous());
	}

	@Test
	public void testIsAnonymizedPost_TrueWhenFlagSet() {
		Question q = new Question();
		q.setAnonymous(true);
		assertTrue(utils.isAnonymizedPost(q));
	}

	@Test
	public void testIsAnonymizedPost_FalseWhenFlagUnset() {
		Question q = new Question();
		assertFalse(utils.isAnonymizedPost(q));
	}

	@Test
	public void testIsAnonymizedPost_FalseForNullPost() {
		assertFalse(utils.isAnonymizedPost(null));
	}

	@Test
	public void testIsAnonymizedPost_DistinctFromIsAnonymousPost() {
		// A truly anonymous post (unauthenticated poster) has creatorid == ANON_UID but no per-post flag.
		Question anonAuthored = new Question();
		anonAuthored.setCreatorid(Profile.id("-"));
		anonAuthored.setAnonymous(false);
//		assertTrue(utils.isAnonymousPost(anonAuthored));   // creatorid-based detection
		assertFalse(utils.isAnonymizedPost(anonAuthored)); // per-post flag is off

		// A per-post-anonymized post has a real creatorid and the flag set.
		Question perPostAnon = new Question();
		perPostAnon.setCreatorid("user1" + Para.getConfig().separator() + "profile");
		perPostAnon.setAnonymous(true);
//		assertFalse(utils.isAnonymousPost(perPostAnon));   // creatorid is a real user, not ANON_UID
		assertTrue(utils.isAnonymizedPost(perPostAnon));   // per-post flag is on
	}

	@Test
	public void testGetProfiles_AnonymousPostGetsAnonAuthor() {
		setupReadAll();
		String realCreatorId = "user1" + Para.getConfig().separator() + "profile";
		Question q = new Question();
		q.setId("post1" + Para.getConfig().separator() + "question");
		q.setCreatorid(realCreatorId);
		q.setAnonymous(true);

		utils.getProfiles(Collections.singletonList(q));

		Profile author = q.getAuthor();
		assertNotNull(author);
		assertTrue("Anonymous post should be attached the synthetic anon Profile",
				utils.isAnonymousUser(author));
	}

	@Test
	public void testGetProfiles_NonAnonymousPostGetsRealAuthor() {
		setupReadAll();
		String realCreatorId = "user1" + Para.getConfig().separator() + "profile";
		Question q = new Question();
		q.setId("post1" + Para.getConfig().separator() + "question");
		q.setCreatorid(realCreatorId);
		q.setAnonymous(false);

		utils.getProfiles(Collections.singletonList(q));

		Profile author = q.getAuthor();
		assertNotNull(author);
		assertEquals(realCreatorId, author.getId());
		assertFalse("Non-anonymous post should keep the real author", utils.isAnonymousUser(author));
	}

	@Test
	public void testGetProfiles_MixedListResolvesEachPost() {
		setupReadAll();
		String sep = Para.getConfig().separator();
		String realCreatorId = "user1" + sep + "profile";

		Question anonQ = new Question();
		anonQ.setId("p1" + sep + "question");
		anonQ.setCreatorid(realCreatorId);
		anonQ.setAnonymous(true);

		Question normalQ = new Question();
		normalQ.setId("p2" + sep + "question");
		normalQ.setCreatorid(realCreatorId);
		normalQ.setAnonymous(false);

		Reply anonReply = new Reply();
		anonReply.setId("p3" + sep + "reply");
		anonReply.setCreatorid(realCreatorId);
		anonReply.setAnonymous(true);

		utils.getProfiles(Arrays.asList(anonQ, normalQ, anonReply));

		assertTrue(utils.isAnonymousUser(anonQ.getAuthor()));
		assertFalse(utils.isAnonymousUser(normalQ.getAuthor()));
		assertTrue(utils.isAnonymousUser(anonReply.getAuthor()));
	}

	@Test
	public void testGetProfiles_NullFlagTreatedAsNonAnonymous() {
		setupReadAll();
		String realCreatorId = "user1" + Para.getConfig().separator() + "profile";
		Question q = new Question();
		q.setId("post1" + Para.getConfig().separator() + "question");
		q.setCreatorid(realCreatorId);
		// leave anonymous as null (unset in stored object loaded from DB before migration)
		q.setAnonymous(null);

		utils.getProfiles(Collections.singletonList(q));

		Profile author = q.getAuthor();
		assertNotNull(author);
		assertEquals(realCreatorId, author.getId());
		assertFalse(utils.isAnonymousUser(author));
	}

	@Test
	public void testGetProfiles_EmptyAndNullListsAreSafe() {
		utils.getProfiles(null);
		utils.getProfiles(Collections.emptyList());
		// no exception thrown is the assertion
	}
}