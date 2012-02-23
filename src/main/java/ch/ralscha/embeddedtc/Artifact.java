package ch.ralscha.embeddedtc;

public class Artifact {
	private String groupId;
	private String artifact;

	public Artifact(String groupId, String artifact) {
		this.groupId = groupId;
		this.artifact = artifact;
	}

	@SuppressWarnings("hiding")
	public boolean is(String groupId, String artifact) {
		return this.groupId.equalsIgnoreCase(groupId) && this.artifact.equalsIgnoreCase(artifact);
	}
}