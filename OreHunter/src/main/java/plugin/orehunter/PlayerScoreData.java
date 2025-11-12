package plugin.orehunter;


import java.io.InputStream;
import java.util.List;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import plugin.orehunter.mapper.PlayerScoreMapper;
import plugin.orehunter.mapper.data.PlayerScore;

/**
 * DB接続やそれに付随する登録、更新処理を行うクラスです。
 */
public class PlayerScoreData {

  private final SqlSessionFactory sqlSessionFactory;
  private final PlayerScoreMapper mapper;

  public PlayerScoreData() {
    try {
      InputStream inputStream = Resources.getResourceAsStream("mybatis-config.xml");
      this.sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
      SqlSession session = sqlSessionFactory.openSession(true);
      this.mapper = session.getMapper(PlayerScoreMapper.class);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }


  /**
   * プレイヤースコアテーブルからスコア情報を一覧で取得します。
   *
   * @return スコア情報の一覧
   */
  public List<PlayerScore> selectList() {
      return mapper.selectList();
  }


  /**
   * プレイヤースコアテーブルにスコア情報を登録します。
   *
   * @param playerScore 　プレイヤースコア
   */
  public void insert(PlayerScore playerScore) {
      mapper.insert(playerScore);
  }
}

