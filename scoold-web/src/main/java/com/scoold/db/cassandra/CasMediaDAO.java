package com.scoold.db.cassandra;

import com.scoold.core.Comment;
import com.scoold.core.Media;
import com.scoold.core.Media.MediaType;
import com.scoold.db.AbstractDAOFactory;
import com.scoold.db.AbstractMediaDAO;
import com.scoold.db.cassandra.CasDAOFactory.CF;
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
public final class CasMediaDAO<T, PK> extends AbstractMediaDAO<Media, Long> {

    private static final Logger logger = Logger.getLogger(CasMediaDAO.class.getName());
	private CasDAOUtils cdu = new CasDAOUtils();
	
    public CasMediaDAO () { }

    public Media read (Long id) {
		return cdu.read(Media.class, id.toString(),
				CasDAOFactory.MEDIA);
    }

    public Media read (String uuid) {
        ArrayList<Media> media = cdu.readAll(Media.class, uuid, 
				CasDAOFactory.MEDIA_UUIDS, CasDAOFactory.MEDIA, String.class,
				null, null, null, 1, true, false, false);

		if(media == null || media.isEmpty()) return null;

		return media.get(0);
    }

    public Long create (Media newMedia) {
		int count = 0;
		CF<Long> linkerCF = null;
		String parentuuid = newMedia.getParentuuid();
		Mutator<String> mut = CasDAOUtils.createMutator();

		Long id = cdu.create(newMedia, CasDAOFactory.MEDIA, mut);
		String idstr = id.toString();

		switch(newMedia.getMediaType()){
			case PHOTO:
				linkerCF = CasDAOFactory.PHOTOS;
				count = cdu.countColumns(parentuuid,
						CasDAOFactory.PHOTOS, Long.class);

				// check max photo limit
//				if(count >= CasDAOFactory.MAX_PHOTOS_PER_UUID){
//					deleteLastMedia(parentuuid, newMedia.getMediaType());
//				}

				break;
			default:
				linkerCF = CasDAOFactory.DRAWER;
				count = cdu.countColumns(parentuuid,
						CasDAOFactory.DRAWER, Long.class);

//				if(count >= CasDAOFactory.MAX_DRAWER_ITEMS_PER_UUID){
//					deleteLastMedia(parentuuid, newMedia.getMediaType());
//				}
		}

		if(id != null){
			CasDAOUtils.addInsertions(Arrays.asList(new Column[]{
				new Column(newMedia.getUuid(), CasDAOFactory.MEDIA_UUIDS, idstr, id.toString()),
				new Column(parentuuid, CasDAOFactory.MEDIA_PARENTUUIDS, id, id.toString()),
				new Column(parentuuid, linkerCF, id, id.toString())
			}), mut);

			updateOrCreateLabels(newMedia, mut);
		}

		mut.execute();

		return id;
    }

    public void update(Media transientMedia) {
		Mutator<String> mut = CasDAOUtils.createMutator();
		updateOrCreateLabels(transientMedia, mut);
		cdu.update(transientMedia, CasDAOFactory.MEDIA, mut);
		mut.execute();
    }

    public void delete(Media persistentMedia) {
		Media media = persistentMedia;
		Mutator<String> mut = CasDAOUtils.createMutator();

		if(media.getUuid() == null || media.getType() == null ){
			media = read(media.getId());
			if(media == null) return;
		}

		String uuid = media.getUuid();
		String parentuuid = media.getParentuuid();
		Long id = media.getId();
		Set<String> labelset = media.getLabelsSet();

		CF<Long> cf = (media.getMediaType() == MediaType.PHOTO) ?
			CasDAOFactory.PHOTOS : CasDAOFactory.DRAWER;

		for (String label : labelset) {
			deleteLabel(label, id, parentuuid, mut);
		}

		// delete the media object
		cdu.delete(media, CasDAOFactory.MEDIA, mut);
		// delete all comments for media
		new CasCommentDAO<Comment, Long>().deleteAllCommentsForUUID(uuid, mut);
		// delete linkers
		cdu.deleteRow(uuid, CasDAOFactory.MEDIA_UUIDS, mut);
		CasDAOUtils.addDeletion(new Column<Long, String>(parentuuid,
				CasDAOFactory.MEDIA_PARENTUUIDS, id, null), mut);
		cdu.deleteRow(parentuuid, cf, mut);
		
		mut.execute();
    }

	public ArrayList<Media> readAllMediaForUUID (String uuid, MediaType  type,
			String label, MutableLong  page, MutableLong  itemcount,  boolean reverse){
		return readAllMediaForUUID(uuid, type, label, page, itemcount,
				CasDAOFactory.MAX_ITEMS_PER_PAGE, reverse);
	}

	public ArrayList<Media> readAllMediaForUUID (String parentUUID, MediaType  type,
						String label, MutableLong page, MutableLong  itemcount,
						int maxPerPage,  boolean reverse){
		ArrayList<Media> list = null;

		if (StringUtils.isBlank(label)) {
			CF<Long> colFamily = (type == MediaType.PHOTO) ?
				CasDAOFactory.PHOTOS : CasDAOFactory.DRAWER;

			list = cdu.readAll(Media.class, parentUUID, colFamily,
					CasDAOFactory.MEDIA, Long.class, CasDAOUtils.toLong(page),
					page, itemcount, maxPerPage, reverse, false, true);
		} else {
			//read keys from Labels and join with photos
			String key = label.concat(AbstractDAOFactory.SEPARATOR).concat(parentUUID);
			list = cdu.readAll(Media.class, key, CasDAOFactory.LABELS_MEDIA, 
				CasDAOFactory.MEDIA, Long.class, CasDAOUtils.toLong(page), 
				page, itemcount, maxPerPage, reverse, true, true);
		}
		return (list == null) ? new ArrayList<Media>(0) : list;
	}

	public ArrayList<String> readAllLabelsForUUID (String uuid){
		List<HColumn<String, String>> labels = cdu.readRow(uuid,
				CasDAOFactory.LABELS, String.class, null, null, null,
				CasDAOFactory.DEFAULT_LIMIT, false);

		ArrayList<String> list = new ArrayList<String>();
		for (HColumn<String, String> label : labels) {
			list.add(label.getName());
		}

		return list;
	}

	public ArrayList<Media> readAllSortedBy(String sortColumnFamilyName, MutableLong page,
			MutableLong itemcount, boolean reverse) {
		throw new UnsupportedOperationException("not supported");
	}

	public ArrayList<Media> readPhotosAndCommentsForUUID (String uuid,
				String label, Long photoid, int nextPrevAll, MutableLong itemcount) {

		ArrayList<Media> photos = new ArrayList<Media>(3);
		MediaType type = MediaType.PHOTO;
		
		switch(nextPrevAll){
			case 1:		// get only the next one and this
				photos = readAllMediaForUUID(uuid, type, label, new MutableLong(photoid), itemcount, 1, true);
				if(photos.size() == 1){
					ArrayList<Media> next = readAllMediaForUUID(uuid, type, label, new MutableLong(0), null, 1, true);
					if(!next.isEmpty()) photos.add(next.get(0));
				}
				break;
			case -1:	// get only the prev one and this
				photos = readAllMediaForUUID(uuid, type, label, 
						new MutableLong(photoid), itemcount, 1, false);
				if(photos.size() == 1){
					ArrayList<Media> prev = readAllMediaForUUID(uuid, type, label, 
							new MutableLong(0), null, 1, false);
					if(!prev.isEmpty()) photos.add(prev.get(0));
				}
				break;
			default:	// get all 3
				Media curr = new Media();
				Media next = new Media();
				Media prev = new Media();

				//read current and get next id
				photos = readAllMediaForUUID(uuid, type, label, 
						new MutableLong(photoid), itemcount, 1, true);	
				
				if(photos.isEmpty()) return photos;
				
				curr = photos.get(0);

				// CASE 1: one photo in list
				if(itemcount.longValue() == 1){
					prev = curr;
					next = curr;
				}else if(itemcount.longValue() == 2){
				// CASE 2: two photos in list
					if (photos.size() == 1) {
						photos.clear();
						photos = readAllMediaForUUID(uuid, type, label, 
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
						photos = readAllMediaForUUID(uuid, type, label, 
								new MutableLong(0), null, 1, true);
						next = photos.get(0);
						// reverse to get previous
						photos.clear();
						photos = readAllMediaForUUID(uuid, type, label, 
								new MutableLong(photoid), null, 1, false);
						prev = photos.get(1);
					} else if(photos.size() == 2) {
						next = photos.get(1);
						// reverse to get previous
						photos.clear();
						photos = readAllMediaForUUID(uuid, type, label, 
								new MutableLong(photoid), null, 1, false);
						if (photos.size() == 1) {
							//get last
							photos.clear();
							photos = readAllMediaForUUID(uuid, type, label, 
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

				break;
		}


		HashSet<Long> mids = new HashSet<Long>(3);
		// read comments for every photo in list
		for (Media photo : photos) {
			if(mids.contains(photo.getId())) continue;
			MutableLong commentCount = new MutableLong(0);
			MutableLong commentPage = new MutableLong(0);

			CasCommentDAO<Comment, Long> cdao = new CasCommentDAO<Comment, Long>();
			ArrayList<Comment> comments = cdao.readAllCommentsForUUID(photo.getUuid(),
					commentPage, commentCount);

			photo.setComments(comments);
			photo.setCommentcount(commentCount.longValue());
			photo.setCommentpage(commentPage.longValue());
			mids.add(photo.getId());
		}

		return photos;
	}

	private void updateOrCreateLabels(Media media, Mutator<String> mut) {
		String newLabels = media.getLabels();
		String oldLabels = media.getOldlabels();
		if(StringUtils.isBlank(newLabels) || newLabels.equalsIgnoreCase(oldLabels)) return;
		Map<String, Integer> newLabelIndex = new HashMap<String, Integer>();

		for (String ntag : newLabels.split(",")) {
			newLabelIndex.put(ntag.trim(), 1);
		}

		// sift out all that are unchanged
		if(oldLabels != null){
			for (String olabel : oldLabels.split(",")) {
				olabel = olabel.trim();
				if(newLabelIndex.containsKey(olabel)){
					newLabelIndex.remove(olabel);	// no change so remove
				}else{
					newLabelIndex.put(olabel, -1); // tag missing so deleteRow and update count
				}
			}
		}
		//clean up the empty tag
		newLabelIndex.remove("");

		for (Entry<String, Integer> label : newLabelIndex.entrySet()) {
			switch(label.getValue()){
				case 1:
					// new label added so update
					createLabel(label.getKey(), media.getId(), media.getParentuuid(), mut);
					break;
				case -1:
					// label removed so update
					deleteLabel(label.getKey(), media.getId(), media.getParentuuid(), mut);
					break;
			}
		}
	}

	private void createLabel(String label, Long id, String parentuuid, Mutator<String> mut){
		if(StringUtils.isBlank(label) || StringUtils.isBlank(parentuuid)) return;
		String key = label.concat(AbstractDAOFactory.SEPARATOR).concat(parentuuid);
		CasDAOUtils.addInsertion(new Column<String, String>(parentuuid,
				CasDAOFactory.LABELS, label, label), mut);
		CasDAOUtils.addInsertion(new Column<Long, String>(key,
				CasDAOFactory.LABELS_MEDIA, id, id.toString()), mut);
	}

	private void deleteLabel(String label, Long id, String parentuuid, Mutator<String> mut){
		String key = label.concat(AbstractDAOFactory.SEPARATOR).concat(parentuuid);
		CasDAOUtils.addDeletion(new Column<Long, String>(key, CasDAOFactory.LABELS_MEDIA,
				id, null), mut);

		int count = cdu.countColumns(key, CasDAOFactory.LABELS_MEDIA, Long.class);
		//remove label if no media has it
		if(count <= 0){
			// delete from labels_media
			cdu.deleteRow(key, CasDAOFactory.LABELS_MEDIA, mut);
			// delete from labels
			CasDAOUtils.addDeletion(new Column<String, String>(parentuuid,
					CasDAOFactory.LABELS, label, null), mut);
		}
	}

	private void deleteLastMedia(String parentUUID, MediaType type){
		CF<Long> cf = (type == MediaType.PHOTO) ?
			CasDAOFactory.PHOTOS : CasDAOFactory.DRAWER;

		HColumn<Long, String> lastCol = cdu.getLastColumn(parentUUID,
					cf, Long.class, false);

		if(lastCol != null)
			delete(new Media(lastCol.getName()));
	}

	public void deleteAllMediaForUUID(String parentUUID) {
//		if(true) return;
		Mutator<String> mut = CasDAOUtils.createMutator();
		deleteAllMediaForUUID(parentUUID, mut);
		mut.execute();
	}
	
	protected void deleteAllMediaForUUID(String parentUUID, Mutator<String> mut){
		List<HColumn<Long, String>> keys = cdu.readRow(parentUUID,
				CasDAOFactory.MEDIA_PARENTUUIDS, Long.class,
				null, null, null, CasDAOFactory.DEFAULT_LIMIT, false);

		List<HColumn<String, String>> labels = cdu.readRow(parentUUID,
				CasDAOFactory.LABELS, String.class,
				null, null, null, CasDAOFactory.DEFAULT_LIMIT, false);

		for (HColumn<String, String> label : labels) {
			CasDAOUtils.addDeletion(new Column<Long, String>(label.getName().
					concat(AbstractDAOFactory.SEPARATOR).concat(parentUUID), 
					CasDAOFactory.LABELS_MEDIA), mut);
		}

		for (HColumn<Long, String> hColumn : keys) {
			CasDAOUtils.addDeletion(new Column<String, String>(hColumn.getName().toString(), 
					CasDAOFactory.MEDIA), mut);
		}

		CasDAOUtils.addDeletions(Arrays.asList(new Column[]{
			new Column(parentUUID, CasDAOFactory.MEDIA_PARENTUUIDS),
			new Column(parentUUID, CasDAOFactory.LABELS),
			new Column(parentUUID, CasDAOFactory.PHOTOS),
			new Column(parentUUID, CasDAOFactory.DRAWER)
		}), mut);
	}

}

