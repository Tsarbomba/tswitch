package apiModule;

public class StreamData {

	/** Name of the twitch channel */
	private String name;
	/** status/title of the twitch channel */
	private String status;
	/** url to logo of the twitch channel */
	private String logoUrl;
	/** "Currently playing" of the twitch channel */
	private String game;
	/** Number of viewers on the twitch channel */
	private String viewers;
	/** Whether the twitch channel is online or not. */
	private boolean online;

	public StreamData(final String name, final String logoUrl,
			final String game, final String status, final String viewers,
			final boolean online) {
		super();
		this.status = status;
		this.name = name;
		this.logoUrl = logoUrl;
		this.game = game;
		this.viewers = viewers;
		this.online = online;
	}

	public String getStatus() {
		return status;
	}

	public String getName() {
		return name;
	}

	public String getLogoUrl() {
		return logoUrl;
	}

	public String getGame() {
		return game;
	}

	public String getViewers() {
		return viewers;
	}

	public boolean isOnline() {
		return online;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public void setStatus(final String status) {
		this.status = status;
	}

	public void setLogoUrl(final String logoUrl) {
		this.logoUrl = logoUrl;
	}

	public void setGame(final String game) {
		this.game = game;
	}

	public void setViewers(final String viewers) {
		this.viewers = viewers;
	}

	public void setOnline(final boolean online) {
		this.online = online;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		StreamData other = (StreamData) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return name;
	}
}
