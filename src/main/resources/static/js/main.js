document.addEventListener('DOMContentLoaded', () => {
    // Logger 功能
    const logger = {
        info: (message, data) => {
            console.info(`[INFO] ${message}`, data || '');
            // 可以在这里添加发送日志到服务器的逻辑
        },
        warn: (message, data) => {
            console.warn(`[WARN] ${message}`, data || '');
        },
        error: (message, error) => {
            console.error(`[ERROR] ${message}`, error || '');
            // 可以在这里添加发送错误日志到服务器的逻辑
        },
        debug: (message, data) => {
            console.debug(`[DEBUG] ${message}`, data || '');
        },
        performance: (action, startTime) => {
            const duration = Date.now() - startTime;
            console.info(`[PERFORMANCE] ${action} 耗时: ${duration}ms`);
        }
    };

    // 记录页面加载
    logger.info('页面已加载');

    // 获取DOM元素
    const subscriptionForm = document.getElementById('subscription-form');
    const testGenerateBtn = document.getElementById('test-generate-btn');
    const queryForm = document.getElementById('query-form');
    const subscriptionResults = document.getElementById('subscription-results');
    const noResults = document.getElementById('no-results');
    const subscriptionList = document.getElementById('subscription-list');
    const loadingSpinner = document.getElementById('loading-spinner');
    const modalTitle = document.getElementById('modal-title');
    const modalBody = document.getElementById('modal-body');
    const resultModal = new bootstrap.Modal(document.getElementById('resultModal'));

    // API基础URL
    const API_BASE_URL = '/api/subscriptions';

    // 显示加载动画
    const showLoading = () => {
        logger.debug('显示加载动画');
        loadingSpinner.classList.remove('d-none');
    };

    // 隐藏加载动画
    const hideLoading = () => {
        logger.debug('隐藏加载动画');
        loadingSpinner.classList.add('d-none');
    };

    // 显示模态框
    const showModal = (title, message) => {
        logger.debug(`显示模态框: ${title}`);
        modalTitle.textContent = title;
        modalBody.textContent = message;
        resultModal.show();
    };

    // 处理API错误
    const handleApiError = (error) => {
        logger.error('API错误', error);
        showModal('操作失败', `发生错误: ${error.message || '未知错误'}`);
        hideLoading();
    };

    // 获取表单数据
    const getFormData = () => {
        const timeInput = document.getElementById('scheduleCron').value;
        let scheduleCron = null;
        
        // 如果用户设置了时间，转换为cron表达式
        if (timeInput) {
            const [hours, minutes] = timeInput.split(':');
            scheduleCron = `0 ${minutes} ${hours} * * ?`;
        }
        
        return {
            area: document.getElementById('area').value,
            reader: document.getElementById('reader').value,
            readerEmail: document.getElementById('readerEmail').value,
            scheduleCron: scheduleCron
        };
    };

    // 验证表单
    const validateForm = () => {
        const area = document.getElementById('area').value.trim();
        const reader = document.getElementById('reader').value.trim();
        const readerEmail = document.getElementById('readerEmail').value.trim();
        
        if (!area) {
            showModal('验证失败', '请输入订阅领域');
            return false;
        }
        
        if (!reader) {
            showModal('验证失败', '请输入目标读者');
            return false;
        }
        
        if (!readerEmail) {
            showModal('验证失败', '请输入接收邮箱');
            return false;
        }
        
        // 简单的邮箱格式验证
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        if (!emailRegex.test(readerEmail)) {
            showModal('验证失败', '请输入有效的邮箱地址');
            return false;
        }
        
        return true;
    };

    // 新增订阅
    subscriptionForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        
        if (!validateForm()) {
            return;
        }
        
        logger.info('提交新订阅表单');
        const startTime = Date.now();
        showLoading();

        const subscription = {
            ...getFormData(),
            status: true
        };
        
        logger.debug('订阅数据', subscription);

        try {
            logger.debug('发送创建订阅请求');
            const response = await fetch(API_BASE_URL, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(subscription)
            });

            if (response.ok) {
                const result = await response.json();
                logger.info('订阅创建成功', result);
                
                // 获取发送时间的显示文本
                let timeText = '每日08:00';
                if (result.scheduleCron) {
                    const cronParts = result.scheduleCron.split(' ');
                    if (cronParts.length >= 4) {
                        const hours = cronParts[2].padStart(2, '0');
                        const minutes = cronParts[1].padStart(2, '0');
                        timeText = `每日${hours}:${minutes}`;
                    }
                }
                
                showModal('订阅成功', `您已成功订阅「${result.area}」内容，文章将${timeText}发送至: ${result.readerEmail}`);
                subscriptionForm.reset();
            } else {
                const error = await response.json();
                logger.warn('订阅创建失败', error);
                showModal('订阅失败', error.message || '创建订阅失败，请稍后再试');
            }
        } catch (error) {
            handleApiError(error);
        } finally {
            hideLoading();
            logger.performance('创建订阅操作', startTime);
        }
    });

    // 测试文章生成并发送
    testGenerateBtn.addEventListener('click', async () => {
        if (!validateForm()) {
            return;
        }
        
        logger.info('点击测试文章生成按钮');
        
        const testData = getFormData();
        logger.debug('测试数据', testData);
        
        // 显示短暂的加载动画
        showLoading();
        
        try {
            // 发送测试请求
            fetch(`${API_BASE_URL}/test/generate-and-send`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(testData)
            }).then(response => {
                logger.debug('测试请求已发送，服务器正在处理');
            }).catch(error => {
                logger.error('测试请求发送失败', error);
            });
            
            // 不等待响应，直接显示成功消息
            setTimeout(() => {
                hideLoading();
                showModal('请求已提交', '文章正在生成，完成后将自动发送至您提供的邮箱地址。');
            }, 1000);
            
        } catch (error) {
            handleApiError(error);
            hideLoading();
        }
    });

    // 查询订阅
    queryForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        logger.info('提交查询订阅表单');
        const startTime = Date.now();
        showLoading();

        const email = document.getElementById('queryEmail').value;
        logger.debug(`查询邮箱: ${email}`);

        try {
            logger.debug('发送查询订阅请求');
            const response = await fetch(`${API_BASE_URL}?readerEmail=${encodeURIComponent(email)}`);
            
            if (response.ok) {
                const subscriptions = await response.json();
                logger.debug('查询结果', subscriptions);
                
                // 清空现有结果
                subscriptionList.innerHTML = '';
                
                if (subscriptions && subscriptions.length > 0) {
                    // 显示结果区域
                    subscriptionResults.classList.remove('d-none');
                    noResults.classList.add('d-none');
                    
                    // 填充订阅列表
                    subscriptions.forEach((subscription, index) => {
                        const row = document.createElement('tr');
                        row.dataset.id = subscription.id; // 保存真实ID为dataset属性
                        
                        // 获取发送时间的显示文本
                        let timeText = '08:00';
                        if (subscription.scheduleCron) {
                            const cronParts = subscription.scheduleCron.split(' ');
                            if (cronParts.length >= 4) {
                                const hours = cronParts[2].padStart(2, '0');
                                const minutes = cronParts[1].padStart(2, '0');
                                timeText = `${hours}:${minutes}`;
                            }
                        }
                        
                        row.innerHTML = `
                            <td>${index + 1}</td>
                            <td>${subscription.area}</td>
                            <td>${subscription.reader}</td>
                            <td>${subscription.status ? '<span class="badge bg-success">启用</span>' : '<span class="badge bg-secondary">禁用</span>'}</td>
                            <td>${timeText}</td>
                            <td>
                                <div class="btn-group" role="group">
                                    <button class="btn btn-sm btn-primary edit-btn">修改</button>
                                    <button class="btn btn-sm btn-danger delete-btn">删除</button>
                                </div>
                            </td>
                        `;
                        
                        subscriptionList.appendChild(row);
                    });
                    
                    // 绑定操作按钮事件
                    bindActionButtons();
                } else {
                    // 显示无结果提示
                    subscriptionResults.classList.add('d-none');
                    noResults.classList.remove('d-none');
                }
            } else {
                const error = await response.json();
                showModal('查询失败', error.message || '查询订阅失败，请稍后再试');
            }
        } catch (error) {
            handleApiError(error);
        } finally {
            hideLoading();
            logger.performance('查询订阅操作', startTime);
        }
    });

    // 绑定操作按钮事件
    const bindActionButtons = () => {
        // 删除按钮
        document.querySelectorAll('.delete-btn').forEach(btn => {
            btn.addEventListener('click', async function() {
                const id = this.closest('tr').dataset.id; // 从dataset中获取真实ID
                
                if (confirm('确定要删除这个订阅吗？此操作不可撤销。')) {
                    showLoading();
                    
                    try {
                        const response = await fetch(`${API_BASE_URL}/${id}`, {
                            method: 'DELETE'
                        });
                        
                        if (response.ok) {
                            this.closest('tr').remove();
                            
                            showModal('操作成功', '订阅已成功删除');
                            
                            // 检查是否还有订阅
                            if (subscriptionList.children.length === 0) {
                                subscriptionResults.classList.add('d-none');
                                noResults.classList.remove('d-none');
                            } else {
                                // 重新编号
                                Array.from(subscriptionList.children).forEach((row, index) => {
                                    row.children[0].textContent = index + 1;
                                });
                            }
                        } else {
                            const error = await response.json();
                            showModal('删除失败', error.message || '删除订阅失败，请稍后再试');
                        }
                    } catch (error) {
                        handleApiError(error);
                    } finally {
                        hideLoading();
                    }
                }
            });
        });
        
        // 修改按钮
        document.querySelectorAll('.edit-btn').forEach(btn => {
            btn.addEventListener('click', function() {
                const id = this.closest('tr').dataset.id; // 从dataset中获取真实ID
                const row = this.closest('tr');
                
                // 获取当前值
                const areaCell = row.children[1];
                const readerCell = row.children[2];
                const statusCell = row.children[3];
                const timeCell = row.children[4];
                
                const currentArea = areaCell.textContent;
                const currentReader = readerCell.textContent;
                const currentStatus = statusCell.querySelector('.badge').classList.contains('bg-success');
                const currentTime = timeCell.textContent;
                
                // 提取小时和分钟
                let hours = '08';
                let minutes = '00';
                if (currentTime && currentTime.includes(':')) {
                    [hours, minutes] = currentTime.split(':');
                }
                
                // 创建编辑表单
                const form = document.createElement('form');
                form.className = 'edit-form p-3 bg-light rounded';
                form.innerHTML = `
                    <div class="mb-2">
                        <label class="form-label small">领域</label>
                        <input type="text" class="form-control form-control-sm" id="edit-area" value="${currentArea}">
                    </div>
                    <div class="mb-2">
                        <label class="form-label small">目标读者</label>
                        <input type="text" class="form-control form-control-sm" id="edit-reader" value="${currentReader}">
                    </div>
                    <div class="mb-2">
                        <label class="form-label small">发送时间</label>
                        <input type="time" class="form-control form-control-sm" id="edit-time" value="${hours}:${minutes}">
                    </div>
                    <div class="mb-2">
                        <div class="form-check form-switch">
                            <input class="form-check-input" type="checkbox" id="edit-status" ${currentStatus ? 'checked' : ''}>
                            <label class="form-check-label small" for="edit-status">启用订阅</label>
                        </div>
                    </div>
                    <div class="d-flex gap-2 mt-3">
                        <button type="button" class="btn btn-sm btn-success save-btn">保存</button>
                        <button type="button" class="btn btn-sm btn-secondary cancel-btn">取消</button>
                    </div>
                `;
                
                // 创建模态框
                const editModal = document.createElement('div');
                editModal.className = 'modal fade';
                editModal.id = 'editModal';
                editModal.setAttribute('tabindex', '-1');
                editModal.setAttribute('aria-hidden', 'true');
                
                editModal.innerHTML = `
                    <div class="modal-dialog modal-dialog-centered">
                        <div class="modal-content">
                            <div class="modal-header">
                                <h5 class="modal-title">修改订阅</h5>
                                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                            </div>
                            <div class="modal-body">
                                ${form.outerHTML}
                            </div>
                        </div>
                    </div>
                `;
                
                // 添加到文档
                document.body.appendChild(editModal);
                
                // 显示模态框
                const modal = new bootstrap.Modal(editModal);
                modal.show();
                
                // 保存按钮事件
                editModal.querySelector('.save-btn').addEventListener('click', async function() {
                    const newArea = editModal.querySelector('#edit-area').value;
                    const newReader = editModal.querySelector('#edit-reader').value;
                    const newTime = editModal.querySelector('#edit-time').value;
                    const newStatus = editModal.querySelector('#edit-status').checked;
                    
                    if (!newArea || !newReader || !newTime) {
                        showModal('错误', '请填写所有必填字段');
                        return;
                    }
                    
                    const [newHours, newMinutes] = newTime.split(':');
                    const cronExpression = `0 ${newMinutes} ${newHours} * * ?`;
                    
                    showLoading();
                    
                    try {
                        // 获取完整的订阅信息
                        const getResponse = await fetch(`${API_BASE_URL}/${id}`);
                        if (!getResponse.ok) {
                            throw new Error('获取订阅信息失败');
                        }
                        
                        const subscription = await getResponse.json();
                        
                        // 更新订阅信息
                        subscription.area = newArea;
                        subscription.reader = newReader;
                        subscription.status = newStatus;
                        subscription.scheduleCron = cronExpression;
                        
                        // 发送更新请求
                        const response = await fetch(`${API_BASE_URL}/${id}`, {
                            method: 'PUT',
                            headers: {
                                'Content-Type': 'application/json'
                            },
                            body: JSON.stringify(subscription)
                        });
                        
                        if (response.ok) {
                            const result = await response.json();
                            
                            // 关闭模态框
                            modal.hide();
                            editModal.addEventListener('hidden.bs.modal', function() {
                                editModal.remove();
                            });
                            
                            showModal('操作成功', '订阅信息已更新');
                            
                            // 更新UI
                            areaCell.textContent = newArea;
                            readerCell.textContent = newReader;
                            statusCell.innerHTML = newStatus ? 
                                '<span class="badge bg-success">启用</span>' : 
                                '<span class="badge bg-secondary">禁用</span>';
                            timeCell.textContent = `${newHours}:${newMinutes}`;
                        } else {
                            const error = await response.json();
                            showModal('操作失败', error.message || '更新订阅失败，请稍后再试');
                        }
                    } catch (error) {
                        handleApiError(error);
                    } finally {
                        hideLoading();
                    }
                });
                
                // 取消按钮事件
                editModal.querySelector('.cancel-btn').addEventListener('click', function() {
                    modal.hide();
                    editModal.addEventListener('hidden.bs.modal', function() {
                        editModal.remove();
                    });
                });
                
                // 模态框关闭时移除
                editModal.addEventListener('hidden.bs.modal', function() {
                    editModal.remove();
                });
            });
        });
    };

    // 预定义的领域列表
    const knowledgeAreas = [
        // 技术与编程
        "大模型在编程中的应用",
        "人工智能前沿进展",
        "机器学习算法实践",
        "深度学习框架应用",
        "自然语言处理技术",
        "计算机视觉应用",
        "强化学习最新进展",
        "AI模型训练优化",
        "神经网络架构设计",
        "迁移学习应用实践",
        "量子计算最新进展",
        "区块链技术与应用",
        "Web3.0发展动态",
        "智能合约开发",
        "去中心化应用开发",
        "NFT技术与应用",
        "元宇宙技术进展",
        "虚拟现实开发",
        "增强现实应用",
        "混合现实技术",

        // 软件开发
        "微服务架构设计",
        "云原生应用开发",
        "DevOps最佳实践",
        "容器化技术应用",
        "Kubernetes实践",
        "服务网格技术",
        "API设计与管理",
        "RESTful接口设计",
        "GraphQL应用实践",
        "分布式系统设计",
        "高并发系统架构",
        "数据库优化技术",
        "NoSQL数据库应用",
        "消息队列系统",
        "日志收集与分析",
        "监控系统设计",
        "CI/CD流水线搭建",
        "代码质量管理",
        "测试自动化实践",
        "性能优化技术",

        // 云计算与基础设施
        "云计算技术趋势",
        "多云架构设计",
        "边缘计算应用",
        "Serverless架构",
        "云安全最佳实践",
        "容灾备份策略",
        "负载均衡技术",
        "CDN技术应用",
        "云存储解决方案",
        "网络架构设计",

        // 前端技术
        "前端框架发展",
        "响应式设计实践",
        "前端性能优化",
        "PWA应用开发",
        "WebAssembly应用",
        "微前端架构",
        "前端测试实践",
        "JavaScript新特性",
        "CSS新技术应用",
        "浏览器引擎原理",

        // 移动开发
        "移动应用架构",
        "跨平台开发技术",
        "移动性能优化",
        "移动安全实践",
        "移动用户体验",
        "移动支付技术",
        "移动推送技术",
        "移动定位服务",
        "移动离线存储",
        "移动调试技术",

        // 网络安全
        "网络安全趋势",
        "安全架构设计",
        "渗透测试技术",
        "漏洞扫描实践",
        "安全审计方法",
        "加密技术应用",
        "身份认证方案",
        "访问控制策略",
        "安全运维实践",
        "应急响应方案",

        // 数据科学
        "数据科学实践",
        "大数据处理技术",
        "数据挖掘方法",
        "数据可视化",
        "数据治理方案",
        "数据仓库设计",
        "数据湖架构",
        "实时数据处理",
        "数据分析方法",
        "预测分析技术",

        // 物联网
        "物联网技术应用",
        "智能家居技术",
        "工业物联网",
        "车联网技术",
        "智慧城市方案",
        "传感器技术",
        "物联网安全",
        "物联网协议",
        "边缘计算IoT",
        "物联网平台",

        // 通信技术
        "5G/6G通信技术",
        "无线通信技术",
        "光纤通信技术",
        "卫星通信技术",
        "网络协议发展",
        "通信安全技术",
        "移动通信技术",
        "通信系统设计",
        "通信测试方法",
        "通信标准发展",

        // 新能源技术
        "新能源技术科普",
        "太阳能技术",
        "风能利用技术",
        "氢能源技术",
        "储能技术发展",
        "智能电网技术",
        "节能技术应用",
        "碳中和技术",
        "生物质能源",
        "地热能应用",

        // 电动汽车
        "电动汽车技术",
        "电池技术发展",
        "充电技术创新",
        "自动驾驶技术",
        "车载系统开发",
        "新能源汽车",
        "智能驾驶舱",
        "车联网应用",
        "汽车电子技术",
        "充换电系统",

        // 医疗健康
        "医疗AI应用",
        "远程医疗技术",
        "医学影像分析",
        "健康监测技术",
        "医疗大数据",
        "精准医疗技术",
        "生物医学工程",
        "医疗机器人",
        "智慧医疗方案",
        "医疗信息系统",

        // 生物技术
        "生物技术创新",
        "基因编辑技术",
        "生物信息学",
        "合成生物学",
        "蛋白质工程",
        "生物材料",
        "生物能源",
        "生物制药",
        "生物传感器",
        "微生物技术",

        // 教育科技
        "教育科技创新",
        "在线教育平台",
        "教育AI应用",
        "自适应学习",
        "教育游戏化",
        "虚拟课堂技术",
        "教育数据分析",
        "智能题库系统",
        "在线考试技术",
        "教育管理系统",

        // 金融科技
        "金融科技创新",
        "数字货币技术",
        "支付技术创新",
        "金融安全技术",
        "智能投顾系统",
        "区块链金融",
        "保险科技应用",
        "监管科技发展",
        "量化交易技术",
        "金融风控系统",

        // 企业管理
        "企业数字化转型",
        "项目管理方法",
        "敏捷管理实践",
        "知识管理系统",
        "企业架构设计",
        "流程自动化",
        "企业协同办公",
        "人力资源管理",
        "客户关系管理",
        "供应链管理",

        // 创新创业
        "创业商业模式",
        "创新管理方法",
        "产品战略规划",
        "市场营销策略",
        "品牌建设方法",
        "用户增长策略",
        "融资策略指南",
        "创业团队管理",
        "商业计划书",
        "创业风险控制",

        // 可持续发展
        "环保技术创新",
        "可持续发展",
        "循环经济模式",
        "绿色建筑技术",
        "环境监测技术",
        "废物处理技术",
        "清洁能源应用",
        "生态修复技术",
        "低碳技术应用",
        "环境保护策略"
    ];

    // 获取随机领域
    function getRandomArea() {
        const randomIndex = Math.floor(Math.random() * knowledgeAreas.length);
        return knowledgeAreas[randomIndex];
    }

    // AI推荐按钮事件绑定
    const aiSuggestButton = document.getElementById('ai-suggest-area');
    const areaInput = document.getElementById('area');
    
    if (aiSuggestButton && areaInput) {
        logger.debug('绑定AI推荐按钮事件');
        aiSuggestButton.addEventListener('click', () => {
            const randomArea = getRandomArea();
            logger.debug(`生成随机领域: ${randomArea}`);
            areaInput.value = randomArea;
        });
    } else {
        logger.warn('未找到AI推荐按钮或领域输入框');
    }
}); 