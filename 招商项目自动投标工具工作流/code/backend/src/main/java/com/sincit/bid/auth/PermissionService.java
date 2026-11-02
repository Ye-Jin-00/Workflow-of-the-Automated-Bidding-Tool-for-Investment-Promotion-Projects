package com.sincit.bid.auth;

import com.sincit.bid.common.BusinessException;
import com.sincit.bid.domain.BidProject;
import com.sincit.bid.domain.BidStatus;
import com.sincit.bid.domain.UserRole;
import org.springframework.stereotype.Component;

/**
 * 数据权限集中判定：
 * - 管理员：全部数据；
 * - 投标经理：本部门项目（可编辑自己负责的项目）；
 * - 复核人：全部项目只读 + 待复核项目可写复核意见；
 * - 观察员：仅可查看已通过/已提交/已归档项目。
 */
@Component
public class PermissionService {

    public boolean canView(CurrentUser actor, BidProject project) {
        if (actor == null || project == null || project.isDeleted()) {
            return false;
        }
        // 草稿是个人工作底稿：仅负责人与管理员可见
        if (project.getStatus() == BidStatus.DRAFT
                && !actor.isAdmin() && !actor.getId().equals(project.getOwnerId())) {
            return false;
        }
        switch (actor.getRole()) {
            case ADMIN:
                return true;
            case REVIEWER:
                return true;
            case VIEWER:
                return project.getStatus() == BidStatus.APPROVED
                        || project.getStatus() == BidStatus.SUBMITTED
                        || project.getStatus() == BidStatus.ARCHIVED;
            case BID_MANAGER:
                return actor.getId().equals(project.getOwnerId())
                        || (actor.getDepartment() != null
                        && actor.getDepartment().equals(project.getDepartment()));
            default:
                return false;
        }
    }

    public void assertView(CurrentUser actor, BidProject project) {
        if (!canView(actor, project)) {
            throw BusinessException.forbidden("无权查看该项目");
        }
    }

    /**
     * 项目推进类操作：管理员或项目负责人。
     */
    public boolean canOperate(CurrentUser actor, BidProject project) {
        if (actor == null || project == null) {
            return false;
        }
        if (actor.isAdmin()) {
            return true;
        }
        return actor.getRole() == UserRole.BID_MANAGER && actor.getId().equals(project.getOwnerId());
    }

    public void assertCanCreate(CurrentUser actor) {
        if (actor == null || !(actor.isAdmin() || actor.getRole() == UserRole.BID_MANAGER)) {
            throw BusinessException.forbidden("当前角色不能创建项目");
        }
    }

    public void assertOperate(CurrentUser actor, BidProject project) {
        if (!canOperate(actor, project)) {
            throw BusinessException.forbidden("仅项目负责人或管理员可推进该项目");
        }
    }

    /**
     * 复核权限：复核人与管理员可复核，但必须回避自己负责的项目（职责分离）。
     */
    public boolean canReview(CurrentUser actor, BidProject project) {
        if (actor == null || project == null) {
            return false;
        }
        if (actor.getId().equals(project.getOwnerId())) {
            return false;
        }
        return actor.isAdmin() || actor.getRole() == UserRole.REVIEWER;
    }

    public void assertReview(CurrentUser actor, BidProject project) {
        if (!canReview(actor, project)) {
            throw BusinessException.forbidden("无权复核该项目（复核人需回避本人负责的项目）");
        }
    }

    public void assertAdmin(CurrentUser actor) {
        if (actor == null || !actor.isAdmin()) {
            throw BusinessException.forbidden("仅系统管理员可执行该操作");
        }
    }
}
