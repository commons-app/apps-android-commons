package fr.free.nrw.commons.contributions;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface ContributionDao {

    @Query("SELECT * FROM contribution")
    List<Contribution> loadAllContributions();

    @Insert
    public void save(Contribution contribution);

    @Insert
    public void save(List<Contribution> contribution);

    @Delete
    public void delete(Contribution contribution);

    @Query("SELECT * from contribution WHERE contentProviderUri=:uri")
    public List<Contribution> getContributionWithUri(String uri);

    @Query("SELECT * from contribution WHERE filename=:fileName")
    public List<Contribution> getContributionWithTitle(String fileName);

    @Query("UPDATE contribution SET state=:state WHERE state in (:toUpdateStates)")
    public void updateStates(int state, int[] toUpdateStates);

    @Query("Delete FROM contribution")
    void deleteAll();
}
