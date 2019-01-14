DROP INDEX index_Tag_Name;
CREATE INDEX index_TagTrackRelation_Tag_id ON TagTrackRelation(Tag_id);
CREATE INDEX index_TagTrackRelation_Track_id ON TagTrackRelation(Track_id);