package com.erudika.scoold.db.dynamodb;

import com.erudika.para.utils.Utils;
import com.erudika.scoold.core.Media;
import com.erudika.scoold.core.Media.MediaType;
import com.erudika.scoold.db.AbstractMediaDAO;
import java.util.ArrayList;
import me.prettyprint.hector.api.mutation.Mutator;
import org.apache.commons.lang3.mutable.MutableLong;

/**
 *
 * @author alexb
 */
final class DynMediaDAO<T> extends AbstractMediaDAO<Media> {

	
    public DynMediaDAO () { }

    public Media read (String id) {
//		return cdu.read(Media.class, id);
		return null;
    }

    public String create (Media newMedia) {
		return null;
//		String parentid = newMedia.getParentid();
//		Mutator<String> mut = cdu.createMutator();
//		
//		int count = cdu.countColumns(parentid, CasDAOFactory.MEDIA_PARENTS, String.class);
//		if(count > Constants.MAX_MEDIA_PER_ID) return null;
//		
//		String id = cdu.create(newMedia, mut);
//
//		if(id != null){
//			cdu.addInsertion(new Column(parentid, CasDAOFactory.MEDIA_PARENTS, 
//					id, id), mut);
//			updateOrCreateLabels(newMedia, mut);
//		}
//
//		mut.execute();
//
//		return id;
    }

    public void update(Media transientMedia) {
    }

    public void delete(Media persistentMedia) {
    }

	public ArrayList<Media> readAllMediaForID (String parentid, MediaType  type,
			String label, MutableLong  page, MutableLong  itemcount,  boolean reverse){
		return readAllMediaForID(parentid, type, label, page, itemcount,
				Utils.MAX_ITEMS_PER_PAGE, reverse);
	}

	public ArrayList<Media> readAllMediaForID (String parentid, MediaType  type,
						String label, MutableLong page, MutableLong  itemcount,
						int maxPerPage,  boolean reverse){
		return null;
	}

	public ArrayList<String> readAllLabelsForID (String parentid){
		return null;
	}


	public void deleteAllMediaForID(String parentid) {
//		Mutator<String> mut = cdu.createMutator();
//		deleteAllMediaForID(parentid, mut);
//		mut.execute();
	}
	
	protected void deleteAllMediaForID(String parentid, Mutator<String> mut){
//		List<HColumn<String, String>> keys = cdu.readRow(parentid, CasDAOFactory.MEDIA_PARENTS, String.class,
//				null, null, null, Utils.DEFAULT_LIMIT, false);
//
//		List<HColumn<String, String>> labels = cdu.readRow(parentid, CasDAOFactory.LABELS, String.class,
//				null, null, null, Utils.DEFAULT_LIMIT, false);
//
//		for (HColumn<String, String> label : labels) {
//			cdu.addDeletion(new Column<String, String>(label.getName().
//					concat(Utils.SEPARATOR).concat(parentid), 
//					CasDAOFactory.LABELS_MEDIA), mut);
//		}
//
//		for (HColumn<String, String> hColumn : keys) {
//			cdu.addDeletion(new Column<String, String>(hColumn.getName(), 
//					CasDAOFactory.OBJECTS), mut);
//		}
//
//		cdu.addDeletions(Arrays.asList(new Column[]{
//			new Column<String, String>(parentid, CasDAOFactory.MEDIA_PARENTS),
//			new Column<String, String>(parentid, CasDAOFactory.LABELS),
//		}), mut);
	}

	public ArrayList<Media> readPhotosAndCommentsForID(String id, String label, Long photoid, MutableLong itemcount) {
		return null;
	}

}

