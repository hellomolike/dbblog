package cn.dblearn.blog.manage.article.service.impl;

import cn.dblearn.blog.common.enums.ModuleEnum;
import cn.dblearn.blog.common.util.PageUtils;
import cn.dblearn.blog.common.util.Query;
import cn.dblearn.blog.manage.article.entity.Article;
import cn.dblearn.blog.manage.article.entity.dto.ArticleDto;
import cn.dblearn.blog.manage.article.entity.vo.ArticleVo;
import cn.dblearn.blog.manage.article.mapper.ArticleMapper;
import cn.dblearn.blog.manage.article.service.ArticleService;
import cn.dblearn.blog.manage.operation.entity.Category;
import cn.dblearn.blog.manage.operation.service.CategoryService;
import cn.dblearn.blog.manage.operation.service.TagService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * articleAdminServiceImpl
 *
 * @author bobbi
 * @date 2018/11/21 12:48
 * @email 571002217@qq.com
 * @description
 */
@Service
public class ArticleServiceImpl extends ServiceImpl<ArticleMapper, Article> implements ArticleService {

    @Autowired
    private TagService tagService;

    @Autowired
    private CategoryService categoryService;

    /**
     * 分页查询博文列表
     *
     * @param params
     * @return
     */
    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        Page<ArticleVo> page = new Query<ArticleVo>(params).getPage();
        List<ArticleVo> articleList = baseMapper.listArticleVo(page, params);
        page.setRecords(articleList);
        // 查询所有分类
        List<Category> categoryList = categoryService.list(new QueryWrapper<Category>().lambda().eq(Category::getType,ModuleEnum.ARTICLE.getValue()));
        // 封装ArticleVo
        articleList.forEach(articleVo -> {
            // 设置类别
            articleVo.setCategoryListStr(categoryService.renderCategoryArr(articleVo.getCategoryId(),categoryList));
            // 设置标签列表
            articleVo.setTagList(tagService.listByLinkId(articleVo.getId(),ModuleEnum.ARTICLE.getValue()));
        });
        return new PageUtils(page);
    }



    /**
     * 保存博文文章
     *
     * @param article
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveArticle(ArticleDto article) {
        baseMapper.insert(article);
        tagService.saveTagAndNew(article.getTagList(),article.getId(),ModuleEnum.ARTICLE.getValue());
    }

    /**
     * 更新博文
     *
     * @param article
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateArticle(ArticleDto article) {
        // 删除多对多所属标签
        tagService.deleteTagLink(article.getId(),ModuleEnum.ARTICLE.getValue());
        // 更新所属标签
        tagService.saveTagAndNew(article.getTagList(),article.getId(), ModuleEnum.ARTICLE.getValue());
        // 更新博文
        baseMapper.updateById(article);
    }

    /**
     * 获取文章对象
     *
     * @param articleId
     * @return
     */
    @Override
    public ArticleDto getArticle(Integer articleId) {
        ArticleDto articleDto = new ArticleDto();
        Article article = this.baseMapper.selectById(articleId);
        BeanUtils.copyProperties(article,articleDto);
        // 查询所属标签
        articleDto.setTagList(tagService.listByLinkId(articleId,ModuleEnum.ARTICLE.getValue()));
        return articleDto;
    }

    /**
     * 批量删除
     *
     * @param articleIds
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteBatch(Integer[] articleIds) {
        //先删除博文标签多对多关联
        Arrays.stream(articleIds).forEach(articleId -> {
            tagService.deleteTagLink(articleId,ModuleEnum.ARTICLE.getValue());
        });
        this.baseMapper.deleteBatchIds(Arrays.asList(articleIds));
    }


}
