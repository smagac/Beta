package core.service;

public interface ILoader extends Service {

	public void setLoadingMessage(String message);
	public void setLoading(boolean loading);
	public boolean isLoading();
}
