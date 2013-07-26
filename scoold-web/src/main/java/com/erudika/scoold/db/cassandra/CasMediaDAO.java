package com.erudika.scoold.db.cassandra;

import com.erudika.para.utils.Utils;
import com.erudika.scoold.core.Comment;
import com.erudika.scoold.core.Media;
import com.erudika.scoold.core.Media.MediaType;
import com.erudika.scoold.db.AbstractMediaDAO;
import com.erudika.scoold.db.cassandra.CasDAOFactory.Column;
import com.erudika.scoold.util.Constants;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Logger;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.mutation.Mutator;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableLong;

/**
 *
 * @author alexb
 */
final class CasMediaDAO<T> extends AbstractMediaDAO<Media> {

    private static final Logger logger = Logger.getLogger(CasMediaDAO.class.getName());
	private CasDAOUtils cdu = new CasDAOUtils(CasDAOFactory.CASSANDRA_PORT);
	private CasCommentDAO<Comment> cdao = new CasCommentDAO<Comment>();
	
    public CasMediaDAO () { }

    public Media read (String id) {
		return cdu.read(Media.class, id);
    }

    public String create (Media newMedia) {
		String parentid = newMedia.getParentid();
		Mutator<String> mut = cdu.createMutator();
		
		int count = cdu.countColumns(parentid, CasDAOFactory.MEDIA_PARENTS, String.class);
		if(count > Constants.MAX_MEDIA_PER_ID) return null;
		
		String id = cdu.create(newMedia, mut);

		if(id != null){
			cdu.addInsertion(new Column(parentid, CasDAOFactory.MEDIA_PARENTS, 
					id, id), mut);
		}

		mut.execute();

		return id;
    }

    public void update(Media transientMedia) {
		Mutator<String> mut = cdu.createMutator();
		cdu.update(transientMedia, mut);
		mut.execute();
    }

    public void delete(Media persistentMedia) {
		Media media = persistentMedia;
		Mutator<String> mut = cdu.createMutator();

		if(media.getId() == null || media.getType() == null ){
			media = read(media.getId());
			if(media == null) return;
		}

		String parentid = media.getParentid();
		String id = media.getId();
		
		// delete the media object
		cdu.delete(media, mut);
		// delete all comments for media
		cdao.deleteAllCommentsForID(id, mut);
		// delete linkers
		cdu.addDeletion(new Column<String, String>(parentid, CasDAOFactory.MEDIA_PARENTS, id, null), mut);
		
		mut.execute();
    }

	public ArrayList<Media> readAllMediaForID (String parentid, MediaType  type,
			String label, MutableLong  page, MutableLong  itemcount,  boolean reverse){
		return readAllMediaForID(parentid, type, label, page, itemcount,
				Utils.MAX_ITEMS_PER_PAGE, reverse);
	}

	public ArrayList<Media> readAllMediaForID (String parentid, MediaType  type,
						String label, MutableLong page, MutableLong  itemcount,
						int maxPerPage,  boolean reverse){
		ArrayList<Media> list = null;

		if (StringUtils.isBlank(label)) {
			list = cdu.readAll(Media.class, null, parentid, 
					CasDAOFactory.MEDIA_PARENTS, String.class, Utils.toLong(page).toString(),
					page, itemcount, maxPerPage, reverse, false, true);
		} else {
			//read keys from Labels and join with photos
			String key = label.concat(Utils.SEPARATOR).concat(parentid);
			list = cdu.readAll(Media.class, null, key, CasDAOFactory.LABELS_MEDIA, 
				String.class, Utils.toLong(page).toString(), 
				page, itemcount, maxPerPage, reverse, true, true);
		}
		return (list == null) ? new ArrayList<Media>(0) : list;
	}

	public ArrayList<String> readAllLabelsForID (String parentid){
		List<HColumn<String, String>> labels = cdu.readRow(parentid,
				CasDAOFactory.LABELS, String.class, null, null, null,
				Utils.DEFAULT_LIMIT, false);

		ArrayList<String> list = new ArrayList<String>();
		for (HColumn<String, String> label : labels) {
			list.add(label.getName());
		}

		return list;
	}

	public ArrayList<Media> readPhotosAndCommentsForID (String parentid,
				String label, Long photoid, MutableLong itemcount) {

		MediaType type = MediaType.PHOTO;
		ArrayList<Media> photos = readAllMediaForID(parentid, type, label, 
				new MutableLong(photoid), itemcount, 1, true);	

		if(photos.isEmpty()) return photos;
		Media curr = photos.get(0);
		Media next = new Media();
		Media prev = new Media();


		// CASE 1: one photo in list
		if(itemcount.longValue() == 1){
			prev = curr;
			next = curr;
		}else if(itemcount.longValue() == 2){
		// CASE 2: two photos in list
			if (photos.size() == 1) {
				photos.clear();
				photos = readAllMediaForID(parentid, type, label, 
						new MutableLong(0), null, 1, true);
				next = photos.get(0);
				prev = next;
			} else if(photos.size() == 2) {
				next = photos.get(1);
				prev = next;
			}
		}else if(itemcount.longValue() >= 3){
		// CASE 3: 3 or more photos in list
			if (photos.size() == 1) {
				photos.clear();
				photos = readAllMediaForID(parentid, type, label, 
						new MutableLong(0), null, 1, true);
				next = photos.get(0);
				// reverse to get previous
				photos.clear();
				photos = readAllMediaForID(parentid, type, label, 
						new MutableLong(photoid), null, 1, false);
				prev = photos.get(1);
			} else if(photos.size() == 2) {
				next = photos.get(1);
				// reverse to get previous
				photos.clear();
				photos = readAllMediaForID(parentid, type, label, 
						new MutableLong(photoid), null, 1, false);
				if (photos.size() == 1) {
					//get last
					photos.clear();
					photos = readAllMediaForID(parentid, type, label, 
							new MutableLong(0), null, 1, false);
					prev = photos.get(0);
				} else if(photos.size() == 2) {
					prev = photos.get(1);
				}
			}
		}

		photos.clear();
		photos.add(curr);
		photos.add(prev);
		photos.add(next);

		HashSet<String> mids = new HashSet<String>(3);
		// read comments for every photo in list
		for (Media photo : photos) {
			if(!mids.contains(photo.getId())){
				MutableLong commentCount = new MutableLong(0);
				MutableLong commentPage = new MutableLong(0);
				ArrayList<Comment> comments = cdao.readAllCommentsForID(photo.getId(), 
						commentPage, commentCount);

				photo.setComments(comments);
				photo.setCommentcount(commentCount.longValue());
				photo.setCommentpage(commentPage.longValue());
				mids.add(photo.getId());
			}
		}

		return photos;
	}

	public void deleteAllMediaForID(String parentid) {
		Mutator<String> mut = cdu.createMutator();
		deleteAllMediaForID(parentid, mut);
		mut.execute();
	}
	
	protected void deleteAllMediaForID(String parentid, Mutator<String> mut){
		List<HColumn<String, String>> keys = cdu.readRow(parentid, CasDAOFactory.MEDIA_PARENTS, String.class,
				null, null, null, Utils.DEFAULT_LIMIT, false);

		List<HColumn<String, String>> labels = cdu.readRow(parentid, CasDAOFactory.LABELS, String.class,
				null, null, null, Utils.DEFAULT_LIMIT, false);

		for (HColumn<String, String> label : labels) {
			cdu.addDeletion(new Column<String, String>(label.getName().
					concat(Utils.SEPARATOR).concat(parentid), 
					CasDAOFactory.LABELS_MEDIA), mut);
		}

		for (HColumn<String, String> hColumn : keys) {
			cdu.addDeletion(new Column<String, String>(hColumn.getName(), 
					CasDAOFactory.OBJECTS), mut);
		}

		cdu.addDeletions(Arrays.asList(new Column[]{
			new Column<String, String>(parentid, CasDAOFactory.MEDIA_PARENTS),
			new Column<String, String>(parentid, CasDAOFactory.LABELS),
		}), mut);
	}

}

