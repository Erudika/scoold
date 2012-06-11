package com.scoold.db.cassandra;

import com.scoold.core.Comment;
import com.scoold.core.Media;
import com.scoold.core.Media.MediaType;
import com.scoold.db.AbstractDAOFactory;
import com.scoold.db.AbstractMediaDAO;
import com.scoold.db.cassandra.CasDAOFactory.Column;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;
import me.prettyprint.hector.api.beans.HColumn;
import me.prettyprint.hector.api.mutation.Mutator;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.mutable.MutableLong;

/**
 *
 * @author alexb
 */
final class CasMediaDAO<T, PK> extends AbstractMediaDAO<Media, Long> {

    private static final Logger logger = Logger.getLogger(CasMediaDAO.class.getName());
	private CasDAOUtils cdu = (CasDAOUtils) CasDAOFactory.getInstance().getDAOUtils();
	private CasCommentDAO<Comment, Long> cdao = new CasCommentDAO<Comment, Long>();
	
    public CasMediaDAO () { }

    public Media read (Long id) {
		return cdu.read(Media.class, id.toString());
    }

    public Long create (Media newMedia) {
		Long parentid = newMedia.getParentid();
		Mutator<String> mut = cdu.createMutator();
		
		int count = cdu.countColumns(parentid.toString(), CasDAOFactory.MEDIA_PARENTS, Long.class);
		if(count > AbstractDAOFactory.MAX_MEDIA_PER_ID) return 0L;
		
		Long id = cdu.create(newMedia, mut);

		if(id != null){
			cdu.addInsertion(new Column(parentid.toString(), CasDAOFactory.MEDIA_PARENTS, 
					id, id.toString()), mut);
			updateOrCreateLabels(newMedia, mut);
		}

		mut.execute();

		return id;
    }

    public void update(Media transientMedia) {
		Mutator<String> mut = cdu.createMutator();
		updateOrCreateLabels(transientMedia, mut);
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

		Long parentid = media.getParentid();
		Long id = media.getId();
		Set<String> labelset = media.getLabelsSet();

		for (String label : labelset) {
			deleteLabel(label, id, parentid, mut);
		}

		// delete the media object
		cdu.delete(media, mut);
		// delete all comments for media
		cdao.deleteAllCommentsForID(id, mut);
		// delete linkers
		cdu.addDeletion(new Column<Long, String>(parentid.toString(),
				CasDAOFactory.MEDIA_PARENTS, id, null), mut);
		
		mut.execute();
    }

	public ArrayList<Media> readAllMediaForID (Long parentid, MediaType  type,
			String label, MutableLong  page, MutableLong  itemcount,  boolean reverse){
		return readAllMediaForID(parentid, type, label, page, itemcount,
				CasDAOFactory.MAX_ITEMS_PER_PAGE, reverse);
	}

	public ArrayList<Media> readAllMediaForID (Long parentid, MediaType  type,
						String label, MutableLong page, MutableLong  itemcount,
						int maxPerPage,  boolean reverse){
		ArrayList<Media> list = null;

		if (StringUtils.isBlank(label)) {
			list = cdu.readAll(Media.class, null, parentid.toString(), 
					CasDAOFactory.MEDIA_PARENTS, Long.class, CasDAOUtils.toLong(page),
					page, itemcount, maxPerPage, reverse, false, true);
		} else {
			//read keys from Labels and join with photos
			String key = label.concat(AbstractDAOFactory.SEPARATOR).concat(parentid.toString());
			list = cdu.readAll(Media.class, null, key, CasDAOFactory.LABELS_MEDIA, 
				Long.class, CasDAOUtils.toLong(page), 
				page, itemcount, maxPerPage, reverse, true, true);
		}
		return (list == null) ? new ArrayList<Media>(0) : list;
	}

	public ArrayList<String> readAllLabelsForID (Long parentid){
		List<HColumn<String, String>> labels = cdu.readRow(parentid.toString(),
				CasDAOFactory.LABELS, String.class, null, null, null,
				CasDAOFactory.DEFAULT_LIMIT, false);

		ArrayList<String> list = new ArrayList<String>();
		for (HColumn<String, String> label : labels) {
			list.add(label.getName());
		}

		return list;
	}

	public ArrayList<Media> readPhotosAndCommentsForID (Long parentid,
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

		HashSet<Long> mids = new HashSet<Long>(3);
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

	private void updateOrCreateLabels(Media media, Mutator<String> mut) {
		String newLabels = media.getLabels() == null ? "" : media.getLabels();
		String oldLabels = media.getOldlabels() == null ? "" : media.getOldlabels();
		if(newLabels.equalsIgnoreCase(oldLabels)) return;
		Map<String, Integer> newLabelIndex = new HashMap<String, Integer>();
 
		for (String ntag : newLabels.split(",")) {
			newLabelIndex.put(ntag.trim(), 1);
		}

		// sift out all that are unchanged
		for (String olabel : oldLabels.split(",")) {
			olabel = olabel.trim();
			if(newLabelIndex.containsKey(olabel)){
				newLabelIndex.remove(olabel);	// no change so remove
			}else{
				newLabelIndex.put(olabel, -1); // tag missing so deleteRow and update count
			}
		}
		//clean up the empty tag
		newLabelIndex.remove("");

		for (Entry<String, Integer> label : newLabelIndex.entrySet()) {
			switch(label.getValue()){
				case 1:
					// new label added so update
					createLabel(label.getKey(), media.getId(), media.getParentid(), mut);
					break;
				case -1:
					// label removed so update
					deleteLabel(label.getKey(), media.getId(), media.getParentid(), mut);
					break;
				default: break;
			}
		}
	}

	private void createLabel(String label, Long id, Long parentid, Mutator<String> mut){
		if(StringUtils.isBlank(label) || parentid == null) return;
		String key = label.concat(AbstractDAOFactory.SEPARATOR).concat(parentid.toString());
		cdu.addInsertion(new Column<String, String>(parentid.toString(),
				CasDAOFactory.LABELS, label, label), mut);
		cdu.addInsertion(new Column<Long, String>(key,
				CasDAOFactory.LABELS_MEDIA, id, id.toString()), mut);
	}

	private void deleteLabel(String label, Long id, Long parentid, Mutator<String> mut){
		String key = label.concat(AbstractDAOFactory.SEPARATOR).concat(parentid.toString());
		cdu.addDeletion(new Column<Long, String>(key, CasDAOFactory.LABELS_MEDIA,
				id, null), mut);

		int count = cdu.countColumns(key, CasDAOFactory.LABELS_MEDIA, Long.class);
		//remove label if no media has it
		if((count - 1) <= 0){
			// delete from labels_media
			cdu.deleteRow(key, CasDAOFactory.LABELS_MEDIA, mut);
			// delete from labels
			cdu.addDeletion(new Column<String, String>(parentid.toString(),
					CasDAOFactory.LABELS, label, null), mut);
		}
	}

	public void deleteAllMediaForID(Long parentid) {
		Mutator<String> mut = cdu.createMutator();
		deleteAllMediaForID(parentid, mut);
		mut.execute();
	}
	
	protected void deleteAllMediaForID(Long parentid, Mutator<String> mut){
		List<HColumn<Long, String>> keys = cdu.readRow(parentid.toString(),
				CasDAOFactory.MEDIA_PARENTS, Long.class,
				null, null, null, CasDAOFactory.DEFAULT_LIMIT, false);

		List<HColumn<String, String>> labels = cdu.readRow(parentid.toString(),
				CasDAOFactory.LABELS, String.class,
				null, null, null, CasDAOFactory.DEFAULT_LIMIT, false);

		for (HColumn<String, String> label : labels) {
			cdu.addDeletion(new Column<Long, String>(label.getName().
					concat(AbstractDAOFactory.SEPARATOR).concat(parentid.toString()), 
					CasDAOFactory.LABELS_MEDIA), mut);
		}

		for (HColumn<Long, String> hColumn : keys) {
			cdu.addDeletion(new Column<String, String>(hColumn.getName().toString(), 
					CasDAOFactory.OBJECTS), mut);
		}

		cdu.addDeletions(Arrays.asList(new Column[]{
			new Column<Long, String>(parentid.toString(), CasDAOFactory.MEDIA_PARENTS),
			new Column<String, String>(parentid.toString(), CasDAOFactory.LABELS),
		}), mut);
	}

}

