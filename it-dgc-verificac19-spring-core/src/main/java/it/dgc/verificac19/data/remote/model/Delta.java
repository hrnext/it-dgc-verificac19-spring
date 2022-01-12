/**
 * 
 */
package it.dgc.verificac19.data.remote.model;

import java.io.Serializable;
import java.util.List;

import com.google.gson.annotations.SerializedName;

/**
 * @author NIGFRA
 *
 */
public class Delta implements Serializable {

    private static final long serialVersionUID = -3370033642587482227L;

    @SerializedName("deletions")
    private List<String> mDeletions;

    @SerializedName("insertions")
    private List<String> mInsertions;

    public List<String> getDeletions() {
        return mDeletions;
    }

    public void setDeletions(List<String> deletions) {
        mDeletions = deletions;
    }

    public List<String> getInsertions() {
        return mInsertions;
    }

    public void setInsertions(List<String> insertions) {
        mInsertions = insertions;
    }

}
