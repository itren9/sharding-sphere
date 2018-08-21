package io.shardingsphere.antlr.utils;

import java.util.List;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

import com.google.common.base.Optional;

import io.shardingsphere.core.parsing.lexer.token.Symbol;
import io.shardingsphere.core.parsing.parser.context.table.Table;
import io.shardingsphere.core.parsing.parser.sql.SQLStatement;
import io.shardingsphere.core.parsing.parser.sql.ddl.AlterTableStatement;
import io.shardingsphere.core.parsing.parser.sql.ddl.ColumnDefinition;
import io.shardingsphere.core.parsing.parser.sql.ddl.create.table.CreateTableStatement;
import io.shardingsphere.core.parsing.parser.token.IndexToken;
import io.shardingsphere.core.parsing.parser.token.TableToken;
import io.shardingsphere.core.util.SQLUtil;
import io.shardingsphere.parser.antlr.mysql.MySQLDDLParser;

public class DDLParserUtils {
	public static void parseTable(SQLStatement statement, ParseTree ddlRootNode) {
		ParserRuleContext tableNameNode = (ParserRuleContext)TreeUtils.getFirstChildByRuleName(ddlRootNode, "tableName");
		if( null != tableNameNode) {
			String name = tableNameNode.getText();
			if(null == name) {
				throw new RuntimeException();
			}
			String dotString = Symbol.DOT.getLiterals();
			int pos = name.lastIndexOf(dotString);
			String literals = name;
			if(pos > 0) {
				literals = name.substring(dotString.length() + 1);
			}else {
				pos = 0;
			}
			
			statement.getSqlTokens().add(new TableToken(tableNameNode.getStart().getStartIndex(), pos, name));
			statement.getTables().add(new Table(SQLUtil.getExactlyValue(literals), Optional.<String>absent()));
		}
	}
	
	public static void parseTableNode(SQLStatement statement, ParserRuleContext tableNameNode) {
		if( null != tableNameNode) {
			String name = tableNameNode.getText();
			if(null == name) {
				throw new RuntimeException();
			}
			String dotString = Symbol.DOT.getLiterals();
			int pos = name.lastIndexOf(dotString);
			String literals = name;
			if(pos > 0) {
				literals = name.substring(dotString.length() + 1);
			}else {
				pos = 0;
			}
			
			statement.getSqlTokens().add(new TableToken(tableNameNode.getStart().getStartIndex(), pos, name));
			statement.getTables().add(new Table(SQLUtil.getExactlyValue(literals), Optional.<String>absent()));
		}
	}
	
	
	public static void parseCreateDefinition(CreateTableStatement statement, ParseTree ddlRootNode) {
		List<ParseTree> createDefinitionNodes =TreeUtils.getAllDescendantByRuleName(ddlRootNode, "createDefinition");
		if(null != createDefinitionNodes) {
			for(ParseTree each : createDefinitionNodes ) {
				if(each.getClass().getSimpleName().startsWith("ColumnNameAndDefinition")) {
					ColumnDefinition column = parseColumnDefinition(each);
					if (null != column) {
						statement.getColumnNames().add(column.getName());
						statement.getColumnTypes().add(column.getType());
						if(column.isPrimaryKey()) {
							statement.getPrimaryKeyColumns().add(column.getName());
						}
					}
				}else if(each.getClass().getSimpleName().startsWith("constraintDefinition")) {
					//TODO add primary key
				}else if(each.getClass().getSimpleName().startsWith("indexDefinition")) {
					//TODO add index
				}
			}
		}
	}
	
	
	
	public static void parseAddColumn(AlterTableStatement statement, ParseTree ddlRootNode) {
		parseSingleColumn(statement, ddlRootNode);

		ParserRuleContext multiColumnNode = (ParserRuleContext) TreeUtils.getFirstChildByRuleName(ddlRootNode,
				"multiColumn");
		if (null == multiColumnNode) {
			return;
		}

		List<ParseTree> columnNodes = TreeUtils.getAllDescendantByRuleName(multiColumnNode, "columnNameAndDefinition");
		if (null == columnNodes) {
			return;
		}

		for (ParseTree each : columnNodes) {
			ColumnDefinition column = parseColumnDefinition(each);
			if (null != column) {
				statement.getAddColumns().add(column);
			}
		}
	}

	public static void parseModifyColumn(AlterTableStatement statement, ParseTree ddlRootNode) {
		ParserRuleContext modifyColumnNode = (ParserRuleContext) TreeUtils.getFirstChildByRuleName(ddlRootNode,
				"modifyColumn");
		if (null == modifyColumnNode) {
			return;
		}

		ParseTree secondChild = modifyColumnNode.getChild(1);
		int start = 1;
		if(secondChild instanceof TerminalNode) {
			start = 2;
		}
		
		ColumnDefinition column = parseColumnDefinition(modifyColumnNode.getChild(start));
		
		if(null != column) {
			statement.getUpdateColumns().put(column.getName(), column);
		}
	}

	
	public static void parseAddIndex(AlterTableStatement statement, ParseTree ddlRootNode) {
		ParserRuleContext indexDefOptionNode = (ParserRuleContext) TreeUtils.getFirstChildByRuleName(ddlRootNode,
				"indexDefOption");
		if (null != indexDefOptionNode) {
			ParserRuleContext indexNameNode = parseTableIndexNode(statement, indexDefOptionNode);
			if(null != indexNameNode) {
				statement.getAddIndexs().add(indexNameNode.getText());
			}
		}
	}
	
	public static ParserRuleContext parseTableIndexNode(SQLStatement statement, ParseTree ancestorNode) {
		if (null != ancestorNode) {
			ParserRuleContext indexNameNode = (ParserRuleContext) TreeUtils.getFirstChildByRuleName(ancestorNode,
					"indexName");
			if (null != indexNameNode) {
				statement.getSqlTokens().add(new IndexToken(indexNameNode.getStart().getStartIndex(),
						indexNameNode.getText(), statement.getTables().getSingleTableName()));
				return indexNameNode;
			}
		}
		
		return null;
	}
	
	public static void parseDropIndex(AlterTableStatement statement, ParseTree ddlRootNode) {
		ParserRuleContext dropIndexDefNode = (ParserRuleContext) TreeUtils.getFirstChildByRuleName(ddlRootNode,
				"dropIndexDef");
		if (null != dropIndexDefNode) {
			ParserRuleContext indexNameNode = (ParserRuleContext)dropIndexDefNode.getChild(dropIndexDefNode.getChildCount() - 1);
			if (null != indexNameNode) {
				statement.getDropIndexs().add(indexNameNode.getText());
				statement.getSqlTokens().add(new IndexToken(indexNameNode.getStart().getStartIndex(),
						indexNameNode.getText(), statement.getTables().getSingleTableName()));
			}
		}
	}
	
	public static void parseRenameIndex(AlterTableStatement statement, ParseTree ddlRootNode) {
		ParserRuleContext renameIndexNode = (ParserRuleContext) TreeUtils.getFirstChildByRuleName(ddlRootNode,
				"renameIndex");
		if (null != renameIndexNode) {
			ParserRuleContext oldIndexNode = (ParserRuleContext)renameIndexNode.getChild(2);
			ParserRuleContext newIndexNode = (ParserRuleContext)renameIndexNode.getChild(renameIndexNode.getChildCount() - 1);
			statement.getSqlTokens().add(new IndexToken(oldIndexNode.getStart().getStartIndex(),
					oldIndexNode.getText(), statement.getTables().getSingleTableName()));
			statement.getSqlTokens().add(new IndexToken(newIndexNode.getStart().getStartIndex(),
					newIndexNode.getText(), statement.getTables().getSingleTableName()));
			statement.getRenameIndexs().put(oldIndexNode.getText(), newIndexNode.getText());
		}
	}
	
	public static void parseRenameTable(AlterTableStatement statement, ParseTree ddlRootNode) {
		ParserRuleContext renameTableNode = (ParserRuleContext) TreeUtils.getFirstChildByRuleName(ddlRootNode,
				"renameTable");
		if (null != renameTableNode) {
			statement.setNewTableName(renameTableNode.getChild(renameTableNode.getChildCount() - 1).getText());
		}
	}

	public static void parseAddPrimaryKey(AlterTableStatement statement, ParseTree ddlRootNode) {
		ParserRuleContext constraintDefinitionNode = (ParserRuleContext) TreeUtils.getFirstChildByRuleName(ddlRootNode,
				"constraintDefinition");
		if (null != constraintDefinitionNode) {
			ParserRuleContext primaryKeyOptionNode = (ParserRuleContext) TreeUtils.getFirstChildByRuleName(ddlRootNode,
					"primaryKeyOption");
			if (null != primaryKeyOptionNode) {
				List<ParseTree> keyPartNodes = TreeUtils.getAllDescendantByRuleName(ddlRootNode, "keyPart");
				if (null != keyPartNodes) {
					for (ParseTree each : keyPartNodes) {
						String columnName = each.getChild(0).getText();
						ColumnDefinition updateColumn = statement.getUpdateColumns().get(columnName);
						if (null == updateColumn) {
							updateColumn = new ColumnDefinition(columnName, null, null, true);
							statement.getUpdateColumns().put(columnName, updateColumn);
						} else {
							updateColumn.setPrimaryKey(true);
						}
					}
				}
			}
		}
	}
	
	public static void parseDropPrimaryKey(AlterTableStatement statement, ParseTree ddlRootNode) {
		ParserRuleContext dropPrimaryKeyNode = (ParserRuleContext) TreeUtils.getFirstChildByRuleName(ddlRootNode,
				"dropPrimaryKey");
		if (null != dropPrimaryKeyNode) {
			statement.setDropPrimaryKey(true);
		}
	}
	

	public static void parseDropColumn(AlterTableStatement statement, ParseTree ddlRootNode) {
		List<ParseTree> dropColumnNodes = TreeUtils.getAllDescendantByRuleName(ddlRootNode, "dropColumn");
		if (null != dropColumnNodes) {
			for (ParseTree each : dropColumnNodes) {
				String columnName = each.getChild(each.getChildCount() - 1).getText();
				if (null != columnName) {
					statement.getDropColumns().add(columnName);
				}
			}
		}
	}

	// TODO parse after | before column
	public static boolean parseSingleColumn(AlterTableStatement statement, ParseTree ddlRootNode) {
		boolean add = false;
		ParserRuleContext singleColumnNode = (ParserRuleContext) TreeUtils.getFirstChildByRuleName(ddlRootNode,
				"singleColumn");
		if (null != singleColumnNode) {
			ParserRuleContext columnNameAndDefinitionNode = (ParserRuleContext) TreeUtils
					.getFirstChildByRuleName(singleColumnNode, "columnNameAndDefinition");
			ColumnDefinition column = parseColumnDefinition(columnNameAndDefinitionNode);
			if (null != column) {
				statement.getAddColumns().add(column);
				add = true;
			}
		}

		return add;
	}

	public static ColumnDefinition parseColumnDefinition(ParseTree columnNameAndDefinitionNode) {
		if (null == columnNameAndDefinitionNode) {
			return null;
		}

		ParserRuleContext columnNameNode = (ParserRuleContext) columnNameAndDefinitionNode.getChild(0);
		ParserRuleContext columnDefinitionNode = (ParserRuleContext) columnNameAndDefinitionNode.getChild(1);
		ParserRuleContext dataTypeRule = (ParserRuleContext) columnDefinitionNode.getChild(0);
		TerminalNode dateType = (TerminalNode) dataTypeRule.getChild(0);
		Integer length = null;
		if (dataTypeRule.getChildCount() > 1) {
			TerminalNode lengthNode = TreeUtils.getFirstTerminalByType(dataTypeRule.getChild(1), MySQLDDLParser.NUMBER);
			if (null != lengthNode) {
				try {
					length = Integer.parseInt(lengthNode.getText());
				} catch (NumberFormatException e) {
				}
			}
		}
		TerminalNode primaryKeyNode = TreeUtils.getFirstTerminalByType(columnDefinitionNode, MySQLDDLParser.PRIMARY);
		boolean primaryKey = false;
		if (null != primaryKeyNode) {
			primaryKey = true;
		}

		return new ColumnDefinition(columnNameNode.getText(), dateType.getText(), length, primaryKey);
	}
}